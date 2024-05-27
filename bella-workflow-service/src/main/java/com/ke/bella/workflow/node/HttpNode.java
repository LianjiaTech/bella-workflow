package com.ke.bella.workflow.node;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.ke.bella.workflow.BellaContext;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Headers.Builder;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import okio.Buffer;

@SuppressWarnings("rawtypes")
public class HttpNode extends BaseNode {
    static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    static OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .build();
    static {
        logging.setLevel(Level.BASIC);
    }

    private Data data;

    public HttpNode(Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .url(buildUrl(context).toURL())
                    .headers(buildHeaders(context))
                    .method(data.getMethod().toUpperCase(), buildBody(context))
                    .build();

            Map processedData = new LinkedHashMap<>();
            processedData.put("request", render(request));

            response = client.newCall(request).execute();

            Map outputs = new LinkedHashMap<>();
            outputs.put("status_code", response.code());
            outputs.put("body", extractBody(response));
            outputs.put("headers", response.headers().toMultimap());
            outputs.put("files", extractFiles(response));
            return NodeRunResult.builder()
                    .processData(processedData)
                    .outputs(outputs)
                    .status(NodeRunResult.Status.succeeded)
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

    private String render(Request request) {
        String template = "{{ request.method() }} {{ request.url().toString() }} HTTP/1.1\n"
                + "{% for header in request.headers().toMultimap().entrySet() %}"
                + "{{ header.key }}: {{ header.value }}\n"
                + "{% endfor %}\n"
                + "\n"
                + "{{ body }}";

        Map<String, Object> map = new HashMap<>();
        map.put("headers", request.headers().names());
        map.put("body", bodyToString(request));
        map.put("request", request);
        return Variables.renderJinjia(template, map);
    }

    private Object extractFiles(Response response) {
        return null; // TODO
    }

    private static String bodyToString(final Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }

    private String extractBody(Response response) throws IOException {
        String type = response.body().contentType().type();
        if(type.contains("text") || type.contains("json") || type.contains("xml")) {
            return response.body().string();
        }
        return "";
    }

    private RequestBody buildBody(WorkflowContext context) {
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

        return null;
    }

    private Headers buildHeaders(WorkflowContext context) {

        Builder builder = new Headers.Builder();
        // common header
        Map<String, String> headers = toFormattedMap(data.getHeaders(), context.getState().getVariablePool());
        headers.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));

        // Auth header
        Data.Authorization.Config config = data.getAuthorization().getConfig();
        String authType = data.getAuthorization().type;
        String apiKey = null;
        if("api-key".equals(authType)) {
            apiKey = data.getAuthorization().getConfig().apiKey;
        } else if("bella-key".equals(authType)) {
            apiKey = BellaContext.getApiKey();
        }

        if(!"no-auth".equals(authType)) {
            builder.add(config.header(), config.prefix() + apiKey);
        }

        return builder.build();
    }

    private URI buildUrl(WorkflowContext context) throws Exception {
        String base = data.getUrl();
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
                    urlEncodeUTF8(entry.getKey().toString()),
                    urlEncodeUTF8(entry.getValue().toString())));
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
        boolean maskAuthorizationHeader = true;

        @Getter
        @Setter
        public static class Authorization {
            @Getter
            @Setter
            public static class Config {
                // 'basic', 'bearer', 'custom'
                String type;
                String apiKey;
                String header;

                public String header() {
                    return StringUtils.isEmpty(header) ? "Authorization" : header;
                }

                public String prefix() {
                    if("basic".equals(type)) {
                        return "Basic ";
                    } else if("bearer".equals(type)) {
                        return "Bearer ";
                    }
                    return "";
                }
            }

            // 'no-auth', 'api-key', 'bella-key'
            String type;
            Config config;
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
            int connect = 300;
            int read = 600;
            int write = 600;
        }
    }
}
