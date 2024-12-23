package com.ke.bella.workflow.tool;

import static okhttp3.internal.Util.EMPTY_REQUEST;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.KeIAM;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;
import okhttp3.logging.HttpLoggingInterceptor;

public class ApiTool implements ITool {
    static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    static OkHttpClient toolHttpClient = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .build();

    static {
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
    }

    private final ToolBundle toolBundle;
    private final Credentials credentials;

    public ApiTool(ToolBundle toolBundle, Credentials credentials) {
        this.toolBundle = toolBundle;
        this.credentials = credentials;
    }

    @Nullable
    private static RequestBody getRequestBody(String method, Map<String, String> headers, Map<String, Object> body) {
        if(!HttpMethod.requiresRequestBody(method)) {
            return null;
        }

        if(headers.containsKey("Content-Type")) {
            String contentType = headers.get("Content-Type");
            if("application/json".equals(contentType)) {
                return RequestBody.create(JsonUtils.toJson(body), MediaType.parse("application/json; charset=utf-8"));
            } else if("application/x-www-form-urlencoded".equals(contentType)) {
                FormBody.Builder builder = new FormBody.Builder();
                for (Map.Entry<String, Object> entry : body.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if(Objects.nonNull(value)) {
                        builder.add(key, value.toString());
                    }
                }
                return builder.build();
            } else if("multipart/form-data".equals(contentType)) {
                okhttp3.MultipartBody.Builder builder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM);
                for (Map.Entry<String, Object> bodyEntry : body.entrySet()) {
                    String key = bodyEntry.getKey();
                    Object value = bodyEntry.getValue();
                    if(Objects.nonNull(value)) {
                        builder.addFormDataPart(key, value.toString());
                    }
                }
                return builder.build();
            } else {
                return RequestBody.create(JsonUtils.toJson(body).getBytes());
            }
        }
        return EMPTY_REQUEST;
    }

    @Override
    public String execute(Map<String, Object> params) {
        // parameters validating
        validate(params);
        return doHttpRequest(toolBundle.getServerUrl(), toolBundle.getMethod().toUpperCase(), params);
    }

    private void validate(Map<String, Object> params) {
        for (ToolBundle.ToolParameter parameter : toolBundle.getParams()) {
            if(parameter.getRequired() && !params.containsKey(parameter.getName())) {
                if(Objects.isNull(parameter.get_default())) {
                    throw new IllegalArgumentException(String.format("missing required parameter: %s", parameter.getName()));
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String doHttpRequest(String serverUrl, String method, Map<String, Object> inputParams) {
        Map<String, String> headers = new HashMap<>();
        Map<String, String> params = new HashMap<>();
        Map<String, String> pathParams = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        Map<String, String> cookies = new HashMap<>();
        if(!CollectionUtils.isEmpty((Collection<?>) toolBundle.getOperation().get("parameters"))) {
            List<Map<String, Object>> parameters = (List) toolBundle.getOperation().get("parameters");
            for (Map parameter : parameters) {
                String paramName = (String) parameter.get("name");
                String paramIn = (String) parameter.get("in");
                Boolean required = (Boolean) parameter.getOrDefault("required", false);
                Object value = null;
                if(inputParams.containsKey(paramName)) {
                    value = inputParams.get(paramName);
                } else if(required) {
                    throw new IllegalArgumentException(String.format("missing required parameter: %s", paramName));
                } else {
                    value = ((Map) parameter.getOrDefault("schema", new HashMap<>())).getOrDefault("default", "");
                }
                if("path".equals(paramIn)) {
                    pathParams.put(paramName, value.toString());
                } else if("query".equals(paramIn)) {
                    params.put(paramName, value.toString());
                } else if("cookie".equals(paramIn)) {
                    cookies.put(paramName, value.toString());
                } else if("header".equals(paramIn)) {
                    headers.put(paramName, value.toString());
                }
            }
        }

        // Check if there is a request body and handle it
        if(toolBundle.getOperation().containsKey("requestBody")) {
            Map<String, Map> requestBody = (Map<String, Map>) toolBundle.getOperation().get("requestBody");
            if(requestBody.containsKey("content")) {
                Map<String, Map> content = requestBody.get("content");
                Map.Entry<String, Map> contentEntry = content.entrySet().iterator().next();

                headers.put("Content-Type", contentEntry.getKey());
                Map<String, Object> bodySchema = (Map<String, Object>) contentEntry.getValue().get("schema");
                Map<String, Object> properties = (Map<String, Object>) bodySchema.getOrDefault("properties", new HashMap<>());
                for (String name : properties.keySet()) {
                    Map<String, Object> property = (Map<String, Object>) properties.get(name);
                    if(inputParams.containsKey(name)) {
                        Object o = convertBodyPropertyType(property, inputParams.get(name));
                        body.put(name, o);
                    } else if(bodySchema.containsKey("required") && ((List<String>) bodySchema.get("required")).contains(name)) {
                        throw new IllegalArgumentException(
                                "Missing required parameter " + name + " in operation " + toolBundle.getOperationId());
                    } else {
                        body.put(name, property.getOrDefault("default", null));
                    }
                }
            }
        }

        // Replace path parameters
        for (String name : pathParams.keySet()) {
            serverUrl = serverUrl.replace("{" + name + "}", String.valueOf(pathParams.get(name)));
        }
        // Parse HTTP body data if needed
        RequestBody requestBody = getRequestBody(method, headers, body);
        Response response = null;
        try {
            // build url
            HttpUrl.Builder urlBuilder = HttpUrl.parse(serverUrl).newBuilder();
            for (Map.Entry<String, String> paramEntry : params.entrySet()) {
                urlBuilder.addQueryParameter(paramEntry.getKey(), paramEntry.getValue());
            }
            HttpUrl url = urlBuilder.build();

            // build headers
            Headers.Builder headerBuilder = new Headers.Builder();
            if(Objects.nonNull(credentials)) {
                if("ke-IAM".equals(credentials.getAuthType())) {
                    String signed = KeIAM.generateAuthorization(credentials.getApiKey(), credentials.getSecret(),
                            RandomStringUtils.randomNumeric(9), method, url.encodedPath(), url.host(), url.query());
                    headers.put(credentials.getKey(), credentials.getPrefix() + signed);
                } else {
                    headers.put(credentials.getKey(), credentials.getPrefix() + credentials.getApiKey());
                }
            }
            headers.forEach(headerBuilder::add);
            cookies.forEach(headerBuilder::add);
            Request request = new Request.Builder()
                    .url(url)
                    .headers(headerBuilder.build())
                    .method(method, requestBody)
                    .build();

            response = toolHttpClient.newCall(request).execute();
            if(response.isSuccessful()) {
                if(response.body() != null) {
                    return response.body().string();
                } else {
                    return "";
                }
            } else {
                throw new IllegalStateException("invoke tool failed, code=" + response.code() + ", message=" + response.message());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object convertBodyPropertyType(Map<String, Object> property, Object value) {
        try {
            if(property.containsKey("type")) {
                String type = (String) property.get("type");
                switch (type) {
                case "integer":
                case "int":
                    return Integer.parseInt(value.toString());
                case "number":
                    if(value.toString().contains(".")) {
                        return Double.parseDouble(value.toString());
                    } else {
                        return Integer.parseInt(value.toString());
                    }
                case "string":
                    return value.toString();
                case "boolean":
                    return Boolean.parseBoolean(value.toString());
                case "null":
                    if(value == null) {
                        return null;
                    }
                    break;
                case "object":
                    if(value instanceof String) {
                        try {
                            return JsonUtils.fromJson((String) value, Map.class);
                        } catch (Exception e) {
                            return value;
                        }
                    } else if(value instanceof Map) {
                        return value;
                    } else {
                        return value;
                    }
                case "array":
                    if(value instanceof String) {
                        try {
                            return JsonUtils.fromJson((String) value, List.class);
                        } catch (Exception e) {
                            return value;
                        }
                    } else if(value instanceof List) {
                        return value;
                    } else {
                        return value;
                    }
                default:
                    throw new IllegalArgumentException("Invalid type " + type + " for property " + property);
                }
            } else if(property.containsKey("anyOf") && property.get("anyOf") instanceof java.util.List) {
                return convertBodyPropertyAnyOf(value, (List<Map<String, Object>>) property.get("anyOf"), 10);
            }
        } catch (Exception e) {
            return value;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Object convertBodyPropertyAnyOf(Object value, List<Map<String, Object>> anyOf, int maxRecursive) {
        if(maxRecursive <= 0) {
            throw new RuntimeException("Max recursion depth reached");
        }
        for (Map<String, Object> option : anyOf) {
            try {
                if(option.containsKey("type")) {
                    String type = (String) option.get("type");
                    switch (type) {
                    case "integer":
                    case "int":
                        return Integer.parseInt(value.toString());
                    case "number":
                        if(value.toString().contains(".")) {
                            return Double.parseDouble(value.toString());
                        } else {
                            return Integer.parseInt(value.toString());
                        }
                    case "string":
                        return value.toString();
                    case "boolean":
                        String boolStr = value.toString().toLowerCase();
                        if("true".equals(boolStr) || "1".equals(boolStr)) {
                            return true;
                        } else if("false".equals(boolStr) || "0".equals(boolStr)) {
                            return false;
                        } else {
                            continue;  // Not a boolean, try next option
                        }
                    case "null":
                        if(value == null || value.toString().isEmpty()) {
                            return null;
                        }
                        break;
                    default:
                        continue;  // Unsupported type, try next option
                    }
                } else if(option.containsKey("anyOf") && option.get("anyOf") instanceof List) {
                    // Recursive call to handle nested anyOf
                    return convertBodyPropertyAnyOf(value, (List<Map<String, Object>>) option.get("anyOf"), maxRecursive - 1);
                }
            } catch (Exception e) {
                continue;  // Conversion failed, try next option
            }
        }
        // If no option succeeded, return the value as is
        return value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Credentials {
        private String authType;
        private String prefix;
        private String key;
        private String apiKey;
        private String secret;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder(toBuilder = true)
    public static class ToolBundle {
        private String serverUrl;
        private String method;
        private String summary;
        private String operationId;
        private List<ToolParameter> params;
        @SuppressWarnings("rawtypes")
        private Map operation;

        @AllArgsConstructor
        @Getter
        public enum ToolParameterType {
            NUMBER("number"),
            BOOLEAN("boolean"),
            STRING("string"),
            ARRAY("array"),
            OBJECT("object");

            private final String value;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder(toBuilder = true)
        public static class ToolParameter {
            private String name;
            private String description;
            @Builder.Default
            private ToolParameterType type = ToolParameterType.STRING;
            private Boolean required;
            // todo 后续若支持其他tool可能需要扩展
            @Builder.Default
            private String form = "llm";
            private Object _default;
            private Double min;
            private Double max;
            private Options options;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder(toBuilder = true)
            public static class Options {
                private String value;
                private String label;
            }
        }
    }
}
