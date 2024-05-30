package com.ke.bella.workflow.api;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

public class WorkflowOps {
    public enum ResponseMode {
        streaming,
        blocking,
        callback;
    }

    @Getter
    @Setter
    public static class WorkflowOp extends Operator {
        String workflowId;
    }

    @Getter
    @Setter
    public static class WorkflowCopy extends Operator {
        String workflowId;
        Long version;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder
    public static class WorkflowSync extends Operator {
        String workflowId;
        String graph;
        String title;
        String desc;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class WorkflowList extends Operator {
        String workflowId;
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
    }

    @Getter
    @Setter
    @SuppressWarnings("rawtypes")
    public static class WorkflowRun extends WorkflowOp {
        Map inputs = new HashMap();
        String responseMode = ResponseMode.streaming.name();
        String callbackUrl;
        String triggerFrom;
    }

    @Getter
    @Setter
    public static class WorkflowRunInfo extends Operator {
        String workflowRunId;
    }

    @Getter
    @Setter
    @SuppressWarnings("rawtypes")
    public static class WorkflowNodeRun extends WorkflowOp {
        Map inputs = new HashMap();
        String nodeId;
        String responseMode = ResponseMode.streaming.name();
    }

    @Getter
    @Setter
    public static class TenantCreate extends Operator {
        String tenantName;
    }
}
