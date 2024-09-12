package com.ke.bella.workflow.api;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ke.bella.workflow.IWorkflowCallback.File;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.utils.JsonUtils;

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
        SCHEDULE,
        KAFKA,
        CUSTOM_API;
    }

    public enum TriggerType {
        SCHD,
        KFKA,
        WBOT;
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
    public static class WorkflowAsApiPublish extends WorkflowOp {
        String summary;
        String desc;
        String operationId;
        String host;
        String path;
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
        String mode;
        Integer status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class WorkflowRunPage extends Operator {
        String workflowId;

        String triggerId;

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

        /** Set of key-value pairs that can be attached to workflow_run. */
        @Builder.Default
        Map metadata = new HashMap();

        @Builder.Default
        String responseMode = ResponseMode.streaming.name();

        String callbackUrl;

        @Builder.Default
        String triggerFrom = TriggerFrom.API.name();

        String triggerId;

        String threadId;
        String query;
        List<File> files;

        String traceId;
        int spanLev;

        @Builder.Default
        int flashMode = 1;

        /** chatfow模式下的选项，当为true时，持久化thread及对应的msg到assistants-api */
        boolean stateful;

        public boolean isFlashMode() {
            return (!ResponseMode.callback.name().equals(responseMode))
                    && (flashMode > 0)
                    && (!TriggerFrom.DEBUG.name().equals(triggerFrom));
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowCApiRun extends WorkflowRun {
        @Builder.Default
        String responseMode = ResponseMode.blocking.name();
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

        String name;
        String desc;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class WorkflowSchedulingOp extends Operator {
        String triggerId;

        Map inputs;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowSchedulingPage extends Operator {
        String triggerId;

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
    public static class WorkflowSchedulingRunPage extends WorkflowSchedulingPage {
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class WorkflowRunResponse {
        String tenantId;
        String workflowId;
        Long workflowVersion;
        String workflowRunId;
        String triggerFrom;
        String query;
        String files;
        Map inputs;
        Map outputs;
        String status;
        String error;
        String responseMode;
        Long cuid;
        String cuName;
        LocalDateTime ctime;

        public static WorkflowRunResponse fromWorkflowRunDB(WorkflowRunDB wf) {
            return WorkflowRunResponse.builder()
                    .tenantId(wf.getTenantId())
                    .workflowId(wf.getWorkflowId())
                    .workflowVersion(wf.getWorkflowVersion())
                    .workflowRunId(wf.getWorkflowRunId())
                    .query(wf.getQuery())
                    .files(wf.getFiles())
                    .inputs(JsonUtils.fromJson(wf.getInputs(), Map.class))
                    .outputs(JsonUtils.fromJson(wf.getOutputs(), Map.class))
                    .status(wf.getStatus())
                    .error(wf.getError())
                    .responseMode(wf.getResponseMode())
                    .cuid(wf.getCuid())
                    .cuName(wf.getCuName())
                    .ctime(wf.getCtime())
                    .build();
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class KafkaTriggerCreate extends WorkflowOp {
        @Builder.Default
        Map inputs = new HashMap();

        /**
         * 条件表达式，
         * 默认内置event变量，代表kafka收到的消息体
         * 可以基于event进行条件判断
         * 符合条件的情况下，才会触发工作流执行
         * 表达式必须返回boolean
         */
        String expression;

        /** 数据源ID */
        String datasourceId;

        /** event作为inputs里的哪一个字段 */
        String inputkey;

        String name;

        String desc;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class WebotTriggerCreate extends WorkflowOp {
        @Builder.Default
        Map inputs = new HashMap();

        /**
         * 群机器人ID，也就是webhook url里的key
         */
        String robotId;

        /**
         * 群聊ID
         */
        String chatId;

        /**
         * 条件表达式，
         * 默认内置event变量，代表企微群机器人收到的消息体
         * 可以基于event进行条件判断
         * 符合条件的情况下，才会触发工作流执行
         * 表达式必须返回boolean
         */
        String expression;

        /** event作为inputs里的哪一个字段 */
        String inputkey;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerDeactivate extends WorkflowOp {
        String triggerId;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerQuery extends WorkflowOp {
        String triggerId;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowTrigger {
        String triggerId;
        String triggerType;
        String name;
        String desc;
        String expression;
        String datasourceId;
        String status;
        String workflowId;
        String inputs;
        String inputKey;
    }
}
