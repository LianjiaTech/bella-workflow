package com.ke.bella.workflow.api.model;

public enum ParameterType {
    FLOAT("float"),
    INT("int"),
    STRING("string"),
    BOOLEAN("boolean"),
    TEXT("text");

    private final String value;

    ParameterType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static ParameterType of(String value) {
        for (ParameterType type : ParameterType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid parameter type: " + value);
    }
}
