package com.ke.bella.workflow.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@ToString
public class BellaResponse<T> {
    private int code;
    private String message;
    private long timestamp;
    private T data;
    private String stacktrace;
}
