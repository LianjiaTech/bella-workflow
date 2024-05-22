package com.ke.bella.workflow;

import java.util.List;
import java.util.Map;

public class Variables {

    @SuppressWarnings("rawtypes")
    public static Object getValue(Map pool, List<String> selector) {
        try {
            Object result = pool;
            for (String key : selector) {
                if(result instanceof Map) {
                    result = ((Map) result).get(key);
                } else {
                    result = result.getClass().getDeclaredField(key).get(result);
                }
                if(result == null) {
                    return null;
                }
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
