package com.ke.bella.workflow.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ke.bella.workflow.api.WorkflowOps;
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

    public void workflowRun(WorkflowSchedulingDB schedulingDb) {
        WorkflowOps.WorkflowRun body = WorkflowOps.WorkflowRun.builder()
                .userId(schedulingDb.getCuid())
                .userName(schedulingDb.getCuName())
                .tenantId(schedulingDb.getTenantId())
                .workflowId(schedulingDb.getWorkflowId())
                .inputs(JsonUtils.fromJson(schedulingDb.getInputs(), Map.class))
                .responseMode(WorkflowOps.ResponseMode.callback.name())
                .triggerFrom(WorkflowOps.TriggerFrom.SCHEDULE.name())
                .callbackUrl(String.format("%s%s%s", workflowDomain, schedulingCallbackPath, schedulingDb.getWorkflowSchedulingId()))
                .build();
        int code = HttpUtils.postJson(String.format("%s%s", workflowDomain, workflowRunPath), body);
        if(!(code >= 200 && code <= 299)) {
            if(500 > code && code >= 400) {
                throw new IllegalArgumentException("code: " + code);
            } else {
                throw new IllegalStateException("code:" + code);
            }
        }
    }
}
