package com.ke.bella.workflow;

import java.util.HashMap;
import java.util.Map;

import com.ke.bella.workflow.api.Operator;

public class BellaContext {
    private static ThreadLocal<Operator> operatorLocal = new ThreadLocal<>();
    private static ThreadLocal<String> apiKey = new ThreadLocal<>();

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
    }

    public static Map<String, Object> snapshot() {
        Map<String, Object> map = new HashMap<>();
        map.put("oper", operatorLocal.get());
        map.put("ak", apiKey.get());
        return map;
    }

    public static void replace(Map<String, Object> map) {
        operatorLocal.set((Operator) map.get("oper"));
        apiKey.set((String) map.get("ak"));
    }
}
