package com.ke.bella.workflow.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class Operator {
    protected Long userId;
    protected String userName;
    protected String tenantId;
}
