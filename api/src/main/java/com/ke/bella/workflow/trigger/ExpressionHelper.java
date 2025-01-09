package com.ke.bella.workflow.trigger;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.aviator.AviatorEvaluator;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.service.code.GroovySandbox;

public class ExpressionHelper {

    public static boolean canTrigger(String scriptsType, String key, String expression, Object value) {
        Map<String, Object> env = new HashMap<>();
        env.put("event", value);
        if(scriptsType.equals(TriggerExpressionType.Aviator.name())) {
            Object res = AviatorEvaluator.execute(key, expression, env, true);
            return res instanceof Boolean && (Boolean) res;
        } else if(scriptsType.equals(TriggerExpressionType.Groovy.name())) {
            Object res = GroovySandbox.execute(expression, env, Configs.MAX_EXE_TIME, Configs.MAX_EXE_MEMORY_ALLOC);
            return res instanceof Boolean && (Boolean) res;
        }
        return false;
    }

    public static void validate(String expressionType, String expression) {
        if(expressionType.equals(TriggerExpressionType.Aviator.name())) {
            AviatorEvaluator.compile(expression);
            return;
        } else if(expressionType.equals(TriggerExpressionType.Groovy.name())) {
            GroovySandbox.compile(expression);
            return;
        }
        throw new IllegalArgumentException("不支持的expressionType");
    }
}
