package com.ke.bella.workflow.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Operator {
    protected Long userId;
    protected String userName;
    protected String tenantId;
}
