package com.ke.bella.workflow.service.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.ke.bella.workflow.service.JsonUtils;
import com.ke.bella.workflow.service.tool.ApiTool.ToolBundle;
import com.ke.bella.workflow.service.tool.ApiTool.ToolBundle.ToolParameter;
import com.ke.bella.workflow.service.tool.ApiTool.ToolBundle.ToolParameterType;

public class OpenapiUtil {

    @SuppressWarnings("unchecked")
    public static ToolBundle extractToolBundleFromOpenapi(Map<String, Object> openapi, String operationId) {
        List<Map<String, Object>> servers = (List<Map<String, Object>>) openapi.get("servers");
        if(servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException("No server found in the openapi yaml.");
        }

        String serverUrl = (String) servers.get(0).get("url");

        Map<String, Object> paths = (Map<String, Object>) openapi.get("paths");
        List<String> methods = Arrays.asList("get", "post", "put", "delete", "patch", "head", "options", "trace");

        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            String path = pathEntry.getKey();
            Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

            for (String method : methods) {
                if(pathItem.containsKey(method)) {
                    Map<String, Object> operation = (Map<String, Object>) pathItem.get(method);

                    if(operationId.equals(operation.get("operationId"))) {
                        return createToolBundle(serverUrl, path, method, operation, openapi);
                    }
                }
            }
        }

        throw new IllegalArgumentException("OperationId not found in the openapi schema.");
    }

    @SuppressWarnings("unchecked")
    public static List<ToolBundle> parseOpenapiToToolBundle(Map<String, Object> openapi) {
        List<Map<String, Object>> servers = (List<Map<String, Object>>) openapi.get("servers");
        if(servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException("No server found in the openapi yaml.");
        }

        String serverUrl = (String) servers.get(0).get("url");

        Map<String, Object> paths = (Map<String, Object>) openapi.get("paths");
        List<String> methods = Arrays.asList("get", "post", "put", "delete", "patch", "head", "options", "trace");

        List<ToolBundle> bundles = new ArrayList<>();
        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            String path = pathEntry.getKey();
            Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

            for (String method : methods) {
                if(pathItem.containsKey(method)) {
                    Map<String, Object> operation = (Map<String, Object>) pathItem.get(method);
                    ToolBundle bundle = createToolBundle(serverUrl, path, method, operation, openapi);
                    bundles.add(bundle);
                }
            }
        }
        return bundles;
    }

    @SuppressWarnings("unchecked")
    public static List<ToolBundle> parseOpenapiToToolBundle(String openapi) {
        return parseOpenapiToToolBundle(JsonUtils.fromJson(openapi, Map.class));
    }

    @SuppressWarnings("unchecked")
    private static ToolBundle createToolBundle(String serverUrl, String path, String method, Map<String, Object> operation,
            Map<String, Object> openapi) {
        List<ToolParameter> parameters = new ArrayList<>();

        if(operation.containsKey("parameters")) {
            List<Map<String, Object>> operationParameters = (List<Map<String, Object>>) operation.get("parameters");
            for (Map<String, Object> parameter : operationParameters) {
                ToolParameterType parameterType = getToolParameterType(parameter);
                Object defaultValue = parameter.containsKey("schema") && ((Map<String, Object>) parameter.get("schema")).containsKey("default")
                        ? ((Map<String, Object>) parameter.get("schema")).get("default")
                        : null;
                ToolParameter toolParameter = new ToolParameter(
                        (String) parameter.get("name"),
                        (String) parameter.getOrDefault("description", ""),
                        parameterType,
                        (Boolean) parameter.getOrDefault("required", false),
                        defaultValue);

                parameters.add(toolParameter);
            }
        }

        if(operation.containsKey("requestBody")) {
            Map<String, Object> requestBody = (Map<String, Object>) operation.get("requestBody");
            if(requestBody.containsKey("content")) {
                Map<String, Object> content = (Map<String, Object>) requestBody.get("content");
                for (Map.Entry<String, Object> contentEntry : content.entrySet()) {
                    Map<String, Object> contentValue = (Map<String, Object>) contentEntry.getValue();

                    if(!contentValue.containsKey("schema")) {
                        continue;
                    }

                    if(contentValue.containsKey("$ref")) {
                        String[] reference = ((String) contentValue.get("$ref")).split("/");
                        Map<String, Object> root = openapi;
                        for (String ref : reference) {
                            root = (Map<String, Object>) root.get(ref);
                        }
                        contentValue.put("schema", root);
                    }

                    if(contentValue.containsKey("schema")) {
                        Map<String, Object> bodySchema = (Map<String, Object>) contentValue.get("schema");
                        List<String> required = bodySchema.containsKey("required") ? (List<String>) bodySchema.get("required")
                                : new ArrayList<>();
                        Map<String, Object> properties = bodySchema.containsKey("properties") ? (Map<String, Object>) bodySchema.get("properties")
                                : new HashMap<>();

                        for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
                            String name = propertyEntry.getKey();
                            Map<String, Object> property = (Map<String, Object>) propertyEntry.getValue();
                            ToolParameterType parameterType = getToolParameterType(property);
                            Boolean propertyRequired = required.stream().anyMatch(s -> s.equals(name));
                            ToolParameter tool = new ToolParameter(
                                    name,
                                    property.getOrDefault("description", "").toString(),
                                    parameterType,
                                    propertyRequired,
                                    property.getOrDefault("default", null));
                            parameters.add(tool);
                        }
                    }
                }
            }
        }

        if(!operation.containsKey("operationId")) {
            if(path.startsWith("/")) {
                path = path.substring(1);
            }
            path = path.replaceAll("[^a-zA-Z0-9_-]", "");
            if(path.isEmpty()) {
                path = UUID.randomUUID().toString();
            }
            operation.put("operationId", path + "_" + method);
        }

        return new ToolBundle(
                serverUrl + path,
                method,
                operation.getOrDefault("description", operation.getOrDefault("summary", "")).toString(),
                (String) operation.get("operationId"),
                parameters,
                operation);
    }

    /**
     * @param parameter
     *
     * @return null if the parameter type is not supported or not set
     */
    @SuppressWarnings("all")
    @Nullable
    private static ToolParameterType getToolParameterType(Map<String, Object> parameter) {
        if(parameter == null) {
            return null;
        }

        String typ = null;
        // openapi2.0
        if(parameter.containsKey("type")) {
            typ = (String) parameter.get("type");
            // openapi3.0
        } else if(parameter.containsKey("schema") && ((Map<String, Object>) parameter.get("schema")).containsKey("type")) {
            typ = (String) ((Map<String, Object>) parameter.get("schema")).get("type");
        }

        if("integer".equals(typ) || "number".equals(typ)) {
            return ToolParameterType.NUMBER;
        } else if("boolean".equals(typ)) {
            return ToolParameterType.BOOLEAN;
        } else if("string".equals(typ)) {
            return ToolParameterType.STRING;
        } else if("array".equals(typ)) {
            return ToolParameterType.ARRAY;
        } else if("object".equals(typ)) {
            return ToolParameterType.OBJECT;
        }
        return null;
    }
}
