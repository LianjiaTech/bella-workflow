package com.ke.bella.workflow.api;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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

    public enum TriggerFrom {
        DEBUG,
        DEBUG_NODE,
        API,
        SCHEDULE;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowOp extends Operator {
        String workflowId;
        Long version;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowPage extends Operator {
        @Builder.Default
        int page = 1;

        @Builder.Default
        int pageSize = 30;

        String name;

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
    @AllArgsConstructor
    public static class WorkflowSync extends Operator {
        String workflowId;
        String graph;
        String title;
        String desc;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class WorkflowRunPage extends Operator {
        String workflowId;

        @Builder.Default
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);

        @Builder.Default
        int page = 1;

        @Builder.Default
        int pageSize = 30;

        String lastId;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class WorkflowRun extends WorkflowOp {
        @Builder.Default
        Map inputs = new HashMap();

        @Builder.Default
        String responseMode = ResponseMode.streaming.name();

        String callbackUrl;

        @Builder.Default
        String triggerFrom = TriggerFrom.DEBUG.name();

        String traceId;
        int spanLev;
    }

    @Getter
    @Setter
    public static class WorkflowRunInfo extends Operator {
        String workflowRunId;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class WorkflowNodeRun extends WorkflowOp {
        @Builder.Default
        Map inputs = new HashMap();
        String nodeId;
        @Builder.Default
        String responseMode = ResponseMode.streaming.name();
    }

    @Getter
    @Setter
    public static class TenantCreate extends Operator {
        String tenantName;
        String parentTenantId;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class WorkflowScheduling extends WorkflowOp {
        @Builder.Default
        Map inputs = new HashMap();
        /**
         * quartz标准的cron表达式
         */
        String cronExpression;
    }
}
