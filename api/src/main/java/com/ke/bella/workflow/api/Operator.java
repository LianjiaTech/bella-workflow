package com.ke.bella.workflow.api;

import org.springframework.util.StringUtils;

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
    protected String email;
    protected String tenantId;
    protected String spaceCode;

    public String getSpaceCode() {
        return StringUtils.isEmpty(spaceCode) ? String.valueOf(userId) : spaceCode;
    }
}
