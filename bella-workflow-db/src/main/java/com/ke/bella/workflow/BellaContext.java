package com.ke.bella.workflow;

import com.ke.bella.workflow.api.Operator;

public class BellaContext {
    private static ThreadLocal<Operator> operatorLocal = new ThreadLocal<>();

    public static void setOperator(Operator operator) {
        operatorLocal.set(operator);
    }

    public static void removeOperator() {
        operatorLocal.remove();
    }

    public static Operator getOperator() {
        return operatorLocal.get();
    }

    public static void clearAll() {
        operatorLocal.remove();
    }
}
