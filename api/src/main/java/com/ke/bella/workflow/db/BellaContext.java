package com.ke.bella.workflow.db;

import java.util.HashMap;
import java.util.Map;

import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.utils.JsonUtils;

public class BellaContext {
    private static ThreadLocal<Operator> operatorLocal = new ThreadLocal<>();
    private static ThreadLocal<String> apiKey = new ThreadLocal<>();
    private static ThreadLocal<Map<String, String>> transHeaders = new ThreadLocal<>();

    public static void setOperator(Operator operator) {
        operatorLocal.set(operator);
    }

    public static Operator getOperator() {
        return operatorLocal.get();
    }

    public static void setApiKey(String ak) {
        apiKey.set(ak);
    }

    public static void clearAll() {
        operatorLocal.remove();
        apiKey.remove();
        transHeaders.remove();
    }

    public static Map<String, Object> snapshot() {
        Map<String, Object> map = new HashMap<>();
        map.put("oper", operatorLocal.get());
        map.put("ak", apiKey.get());
        map.put("headers", transHeaders.get());
        return map;
    }

    public static void replace(Map<String, Object> map) {
        operatorLocal.set((Operator) map.get("oper"));
        apiKey.set((String) map.get("ak"));
        transHeaders.set((Map<String, String>) map.get("headers"));
    }

    public static void replace(String json) {
        Map map = JsonUtils.fromJson(json, Map.class);
        map.put("oper", JsonUtils.convertValue((Map) map.get("oper"), Operator.class));
        replace(map);
    }

    public static String getApiKey() {
        return apiKey.get();
    }

    public static void setTransHeaders(Map<String, String> headers) {
        transHeaders.set(headers);
    }

    public static Map<String, String> getTransHeaders() {
        return transHeaders.get();
    }
}
