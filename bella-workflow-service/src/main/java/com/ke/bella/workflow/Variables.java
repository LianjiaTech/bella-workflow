package com.ke.bella.workflow;

import java.util.List;
import java.util.Map;

public class Variables {

    @SuppressWarnings("rawtypes")
    public static Object getValue(Map pool, List<String> selector) {
        Object result = pool;
        if(!(result instanceof Map)) {
            return null;
        }
        for (String key : selector) {
            result = ((Map) result).get(key);
            if(result == null) {
                return null;
            }
        }
        return result;
    }
}
