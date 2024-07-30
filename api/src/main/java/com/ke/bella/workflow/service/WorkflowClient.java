package com.ke.bella.workflow.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.api.BellaResponse;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;

@Component
public class WorkflowClient {

    @Value("${bella.workflow.domain}")
    private String workflowDomain;

    @Value("${bella.workflow.trigger.scheduling.callback-path}")
    private String schedulingCallbackPath;

    @Value("${bella.workflow.run-path}")
    private String workflowRunPath;

    private static void validate(TenantDB tenantDB, WorkflowSchedulingDB workflowScheduling) {
        if(Objects.isNull(tenantDB) || StringUtils.isEmpty(tenantDB.getOpenapiKey())) {
            String msg = String.format("workflow scheduling failed, reason: tenantId %s is invalid, schedulingId = %s",
                    workflowScheduling.getTenantId(),
                    workflowScheduling.getWorkflowSchedulingId());
            throw new IllegalStateException(msg);
        }
    }

    public WorkflowRunDB workflowRun(TenantDB tenantDB, WorkflowSchedulingDB schedulingDb) {
        validate(tenantDB, schedulingDb);
        WorkflowOps.WorkflowRun body = WorkflowOps.WorkflowRun.builder()
                .userId(schedulingDb.getCuid())
                .userName(schedulingDb.getCuName())
                .tenantId(schedulingDb.getTenantId())
                .workflowId(schedulingDb.getWorkflowId())
                .workflowSchedulingId(schedulingDb.getWorkflowSchedulingId())
                .inputs(JsonUtils.fromJson(schedulingDb.getInputs(), Map.class))
                .responseMode(WorkflowOps.ResponseMode.callback.name())
                .triggerFrom(WorkflowOps.TriggerFrom.SCHEDULE.name())
                .callbackUrl(String.format("%s%s%s", workflowDomain, schedulingCallbackPath, schedulingDb.getWorkflowSchedulingId()))
                .build();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + tenantDB.getOpenapiKey());
        BellaResponse<WorkflowRunDB> bellaResp = HttpUtils.postJson(headers, String.format("%s%s", workflowDomain, workflowRunPath),
                JsonUtils.toJson(body),
                new TypeReference<BellaResponse<WorkflowRunDB>>() {
                });
        if(200 <= bellaResp.getCode() && bellaResp.getCode() < 300) {
            return bellaResp.getData();
        } else {
            throw new IllegalStateException(Optional.ofNullable(bellaResp.getMessage()).orElse("unknown error"));
        }
    }
}
