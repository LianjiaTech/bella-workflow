package com.ke.bella.workflow.api;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

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
    public static class WorkflowSync extends Operator {
        String workflowId;
        String graph;
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
