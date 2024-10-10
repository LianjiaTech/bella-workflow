package com.ke.bella.workflow.api.model;

import lombok.Getter;

public enum ModelStatus {
    ACTIVE("active"),
    NO_CONFIGURE("no-configure"),
    QUOTA_EXCEEDED("quota-exceeded"),
    NO_PERMISSION("no-permission"),
    DISABLED("disabled");

    @Getter
    private final String value;

    ModelStatus(String value) {
        this.value = value;
    }

    public static ModelStatus of(String value) {
        for (ModelStatus status : ModelStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid model status: " + value);
    }
}
