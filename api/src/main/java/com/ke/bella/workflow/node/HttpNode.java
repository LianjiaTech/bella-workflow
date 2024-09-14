package com.ke.bella.workflow.node;

import static okhttp3.internal.Util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.ke.bella.workflow.utils.OpenAiUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.IWorkflowCallback.File;
import com.ke.bella.workflow.IWorkflowCallback.ProgressData;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult.NodeRunResultBuilder;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.node.BaseNode.BaseNodeData;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.KeIAM;
import com.theokanning.openai.service.OpenAiService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Headers.Builder;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.sse.RealEventSource;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okio.Buffer;

@SuppressWarnings("rawtypes")
public class HttpNode extends BaseNode<HttpNode.Data> {
    static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    static OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .build();
    private OkHttpClient exclusiveClient = null;
    static {
        logging.setLevel(Level.BASIC);
    }

    private static final int MAX_RESPONSE_BINARY_SIZE = 10 * 1024 * 1024;

    private static final int MAX_TEXT_SIZE = 1024 * 1024;

    public HttpNode(Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
        exclusiveClient = client.newBuilder()
                .connectTimeout(data.getTimeout().getConnect(), TimeUnit.SECONDS)
                .readTimeout(data.getTimeout().getRead(), TimeUnit.SECONDS)
                .writeTimeout(data.getTimeout().getWrite(), TimeUnit.SECONDS)
                .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Response response = null;
        try {
            Map inputs = new LinkedHashMap<>();
            if(data.getVariables() != null) {
                data.getVariables().forEach(v -> inputs.put(v.getVariable(), context.getState().getVariableValue(v.getValueSelector())));
            }

            Request request = new Request.Builder()
                    .url(buildUrl(context).toURL())
                    .headers(buildHeaders(context))
                    .method(data.getMethod().toUpperCase(), buildBody(context))
                    .build();

            Map processedData = new LinkedHashMap<>();
            processedData.put("request", render(request));

            NodeRunResultBuilder resultBuilder = NodeRunResult.builder()
                    .inputs(inputs)
                    .processData(processedData);

            if(data.isStreaming()) {
                return requestWithSSE(context, request, resultBuilder, callback);
            }

            if(data.isCallback()) {
                return requestWithCallback(context, request, resultBuilder, callback);
            }

            response = exclusiveClient.newCall(request).execute();

            Map outputs = new LinkedHashMap<>();
            int statusCode = response.code();

            ResponseHelper helper = handleResponseBody(request, response);

            outputs.put("status_code", statusCode);
            outputs.put("headers", response.headers().toMultimap());
            outputs.put("files", helper.getFiles());
            outputs.put("body", helper.getBody());
            boolean success = statusCode >= 200 && statusCode <= 299;

            return resultBuilder
                    .outputs(outputs)
                    .status(success ? NodeRunResult.Status.succeeded : NodeRunResult.Status.failed)
                    .error(success ? null : new IllegalStateException(helper.getBody().toString()))
                    .build();
        } catch (Exception e) {
            return NodeRunResult.builder()
                    .status(NodeRunResult.Status.failed)
                    .error(e)
                    .build();
        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    @Override
    public void validate(WorkflowContext ctx) {
        if(ctx.getFlashMode() > 0 && data.isCallback()) {
            throw new IllegalArgumentException("http节点的callback模式下不支持flashMode");
        }
    }

    private ResponseHelper handleResponseBody(Request request, Response response) {
        ResponseHelper result = ResponseHelper.defaultHelper();

        if(Objects.isNull(response.body())) {
            return result;
        }

        boolean isSuccessful = 200 <= response.code() && response.code() <= 299;

        if(isSuccessful) {
            if(HttpUtils.isMIMEFile(response.body().contentType())) {
                List<File> files = extractFiles(request, response);
                result.setFiles(files);
            } else if(Objects.nonNull(data.getResponse()) && "json".equals(data.getResponse().getType())) {
                String bodyStr = extractBody(response);
                Map body = JsonUtils.fromJson(bodyStr, Map.class);
                result.setBody(body);
            } else {
                result.setBody(extractBody(response));
            }
        } else {
            result.setBody(extractBody(response));
        }
        return result;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    @lombok.Data
    private static class ResponseHelper {
        private Object body;
        private List<File> files;

        public static ResponseHelper defaultHelper() {
            return ResponseHelper.builder()
                    .body("")
                    .files(Collections.emptyList())
                    .build();
        }
    }

    private NodeRunResult requestWithCallback(WorkflowContext context, Request request, NodeRunResultBuilder resultBuilder,
            IWorkflowCallback callback) throws IOException {

        Response response = exclusiveClient.newCall(request).execute();
        int statusCode = response.code();
        return resultBuilder
                .status(statusCode >= 200 && statusCode <= 299 ? NodeRunResult.Status.waiting : NodeRunResult.Status.failed)
                .build();
    }

    @SuppressWarnings("unchecked")
    private NodeRunResult requestWithSSE(WorkflowContext context, Request request, NodeRunResultBuilder builder, IWorkflowCallback callback)
            throws InterruptedException {
        CountDownLatch eventLatch = new CountDownLatch(1);
        RealEventSource eventSource = new RealEventSource(request, new EventSourceListener() {
            Map outputs = new LinkedHashMap<>();
            StringBuilder bodyBuffer = new StringBuilder();

            @Override
            public void onOpen(EventSource eventSource, Response response) {
                Map<String, String> data = new LinkedHashMap<>();
                data.put("event", "onOpne");

                callback.onWorkflowNodeRunProgress(context, getNodeId(), nodeRunId, ProgressData.builder()
                        .data(data)
                        .build());

                outputs.put("status_code", response.code());
                outputs.put("headers", response.headers().toMultimap());
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String rawdata) {
                Map<String, Object> data = new LinkedHashMap<>();
                Map<String, String> data2 = new LinkedHashMap<>();
                data.put("event", "onEvent");
                data.put("data", data2);

                data2.put("id", id);
                data2.put("type", type);
                data2.put("data", rawdata);

                callback.onWorkflowNodeRunProgress(context, getNodeId(), nodeRunId, ProgressData.builder()
                        .data(data)
                        .build());
                bodyBuffer.append("\n")
                        .append("id: ").append(id).append("\n")
                        .append("type: ").append(type).append("\n")
                        .append("data: ").append(data).append("\n");
            }

            @Override
            public void onClosed(EventSource eventSource) {
                Map<String, String> data = new LinkedHashMap<>();
                data.put("event", "onClosed");

                callback.onWorkflowNodeRunProgress(context, getNodeId(), nodeRunId, ProgressData.builder()
                        .data(data)
                        .progress(100)
                        .build());

                outputs.put("body", bodyBuffer.toString());
                builder.status(NodeRunResult.Status.succeeded);
                eventLatch.countDown();
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                Map<String, String> data = new LinkedHashMap<>();
                data.put("event", "onFailure");

                callback.onWorkflowNodeRunProgress(context, getNodeId(), nodeRunId, ProgressData.builder()
                        .data(data)
                        .progress(100)
                        .build());

                builder.status(NodeRunResult.Status.failed);
                builder.error(new IllegalStateException("SSE请求异常", t));
                eventLatch.countDown();
            }
        });

        eventSource.connect(exclusiveClient);

        boolean timeout = !eventLatch.await(data.getTimeout().getRead(), TimeUnit.SECONDS);
        if(timeout) {
            builder.error(new IllegalStateException("SSE请求超时间上限"));
        }

        return builder.build();
    }

    private String render(Request request) {
        String template = "{{ request.method() }} {{ request.url().toString() }} HTTP/1.1\n"
                + "{% for header in headers %}"
                + "{{ header.key }}: {{ header.value }}\n"
                + "{% endfor %}\n"
                + "\n"
                + "{{ body }}";

        List<String> headers = request.headers().names().stream()
                .filter(h -> !h.equals("Authorization"))
                .collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        map.put("headers", headers);
        map.put("body", bodyToString(request));
        map.put("request", request);
        return Variables.renderJinjia(template, map);
    }

    private List<File> extractFiles(Request request, Response response) {
        List<File> files = new ArrayList<>();
        ResponseBody body = response.body();
        if(body == null) {
            return files;
        }
        MediaType contentType = body.contentType();
        if(contentType == null) {
            return files;
        }

        if(!HttpUtils.isMIMEFile(contentType)) {
            return files;
        }

        byte[] bytes = HttpUtils.readBodyWithinLimit(body, MAX_RESPONSE_BINARY_SIZE);

        String ext = HttpUtils.getExtensionFromMimeType(contentType);
        String filename = String.format("%s.%s", UUID.randomUUID().toString(), ext);
        OpenAiService service = OpenAiUtils.defaultOpenAiService(BellaContext.getApiKey(), 0, TimeUnit.SECONDS);
        com.theokanning.openai.file.File file = service.uploadFile("assistants", new ByteArrayInputStream(bytes), filename);
        files.add(File.builder()
                .fileId(file.getId())
                .filename(file.getFilename())
                .extension(ext)
                .type(HttpUtils.getFileType(contentType))
                .mimeType(contentType.toString())
                .build());
        return files;
    }

    private static String bodyToString(final Request request) {
        try {
            if(Objects.isNull(request.body())) {
                return null;
            }
            final Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }

    private String extractBody(Response response) {
        ResponseBody body = response.body();
        if(body != null) {
            return new String(HttpUtils.readBodyWithinLimit(body, MAX_TEXT_SIZE), HttpUtils.getCharset(body, StandardCharsets.UTF_8));
        }
        return "";
    }

    private RequestBody buildBody(WorkflowContext context) {
        if(data.getMethod().equalsIgnoreCase("get")) {
            return null;
        }

        String type = data.getBody().getType();
        if("form-data".equals(type)) {
            Map<String, String> values = toFormattedMap(data.getBody().data, context.getState().getVariablePool());
            okhttp3.MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);
            values.forEach(builder::addFormDataPart);
            return builder.build();
        } else if("x-www-form-urlencoded".equals(type)) {
            Map<String, String> values = toFormattedMap(data.getBody().data, context.getState().getVariablePool());
            FormBody.Builder builder = new FormBody.Builder();
            values.forEach(builder::add);
            return builder.build();
        } else if("json".equals(type)) {
            String json = Variables.format(data.getBody().data, context.getState().getVariablePool());
            return RequestBody.create(json, MediaType.parse("application/json"));
        } else if("raw-text".equals(type)) {
            String text = Variables.format(data.getBody().data, context.getState().getVariablePool());
            return RequestBody.create(text, MediaType.parse("text/plain"));
        }

        return EMPTY_REQUEST;
    }

    private Headers buildHeaders(WorkflowContext context) throws Exception {

        Builder builder = new Headers.Builder();
        // common header
        Map<String, String> headers = toFormattedMap(data.getHeaders(), context.getState().getVariablePool());
        headers.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));

        // Auth header
        Data.Authorization.Config config = data.getAuthorization().getConfig();
        String authType = data.getAuthorization().type;
        String apiKey = null;
        if("api-key".equals(authType)) {
            authType = config.getType();
            if("bella".equals(authType)) {
                apiKey = BellaContext.getApiKey();
            } else if("ke-IAM".equalsIgnoreCase(authType)) {
                URL url = buildUrl(context).toURL();
                apiKey = KeIAM.generateAuthorization(config.getApiKey(), config.getSecret(),
                        RandomStringUtils.randomNumeric(9), data.getMethod().toUpperCase(), url.getPath(), url.getHost(), url.getQuery());
            } else {
                apiKey = data.getAuthorization().getConfig().apiKey;
            }
            builder.add(config.header(), config.prefix() + apiKey);
        }

        return builder.build();
    }

    private URI buildUrl(WorkflowContext context) throws Exception {
        String base = Variables.format(data.getUrl(), context.getState().getVariablePool());
        Map<String, String> params = toFormattedMap(data.getParams(), context.getState().getVariablePool());
        String query = mapToUrlParams(params);
        return appendQueryToUrl(base, query);
    }

    private Map<String, String> toFormattedMap(String text, Map pool) {
        Map<String, String> map = new LinkedHashMap<>();
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String t = line.trim();
            if(t.contains(":")) {
                String[] kv = t.split(":", 2);
                if(kv.length == 2) {
                    map.put(kv[0], Variables.format(kv[1], pool));
                } else if(kv.length == 1) {
                    map.put(kv[0], "");
                } else {
                    throw new IllegalArgumentException("配置不合法: " + text);
                }
            }
        }
        return map;
    }

    public static URI appendQueryToUrl(String url, String query) throws URISyntaxException {
        URI oldUri = new URI(url);

        String newQuery = oldUri.getQuery();
        if(newQuery == null) {
            newQuery = query;
        } else {
            newQuery += "&" + query;
        }

        return new URI(oldUri.getScheme(), oldUri.getAuthority(),
                oldUri.getPath(), newQuery, oldUri.getFragment());
    }

    public static String mapToUrlParams(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if(sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    entry.getKey().toString(),
                    entry.getValue().toString()));
        }
        return sb.toString();
    }

    static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static Map<String, Object> defaultConfig(Map<String, Object> filters) {
        return JsonUtils.fromJson(
                "{\"method\":\"get\",\"authorization\":{\"type\":\"no-auth\"},\"body\":{\"type\":\"none\"},\"timeout\":{\"connect\":10,\"read\":60,\"write\":20,\"max_connect_timeout\":300,\"max_read_timeout\":600,\"max_write_timeout\":600}}",
                new TypeReference<Map<String, Object>>() {
                });
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {
        Body body;
        String url;
        String params;
        String method;
        String headers;
        Timeout timeout;
        Authorization authorization;
        String mode = "blocking";
        List<WorkflowSchema.Variable> variables;
        Response response;

        boolean isStreaming() {
            return "streaming".equalsIgnoreCase(mode);
        }

        boolean isCallback() {
            return isWaitCallback() || "callback".equalsIgnoreCase(mode);
        }

        boolean isBlocking() {
            return "blocking".equalsIgnoreCase(mode);
        }

        @Getter
        @Setter
        public static class Authorization {
            @Getter
            @Setter
            public static class Config {
                // 'basic', 'bearer', 'custom', 'bella', 'KE-IAM'
                String type;
                @JsonAlias({ "api_key" })
                String apiKey;
                String secret;
                String header;

                public String header() {
                    return StringUtils.isEmpty(header) ? "Authorization" : header;
                }

                public String prefix() {
                    if("basic".equals(type)) {
                        return "Basic ";
                    } else if("bearer".equals(type)) {
                        return "Bearer ";
                    } else if("bella".equals(type)) {
                        return "Bearer ";
                    }
                    return "";
                }
            }

            // 'no-auth', 'api-key'
            String type;
            Config config;
        }

        @Getter
        @Setter
        public static class Response {
            // 'string', 'json'
            String type;
            String data;
        }

        @Getter
        @Setter
        public static class Body {
            // 'none', 'form-data', 'x-www-form-urlencoded', 'raw-text', 'json'
            String type;
            String data;
        }

        @Getter
        @Setter
        public static class Timeout {
            int connect = 10;
            int read = 300;
            int write = 60;
        }
    }
}
