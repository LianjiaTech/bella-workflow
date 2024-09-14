package com.ke.bella.workflow.api.callbacks;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.api.SseHelper;
import com.ke.bella.workflow.node.NodeType;
import com.ke.bella.workflow.service.WorkflowService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DifyWorkflowRunStreamingCallback extends WorkflowCallbackAdaptor {

    final SseEmitter emitter;
    final WorkflowService ws;
    final long timeout;
    long suspendedTime = System.currentTimeMillis();

    public DifyWorkflowRunStreamingCallback(SseEmitter emitter, WorkflowService ws) {
        this.emitter = emitter;
        this.ws = ws;
        this.timeout = emitter.getTimeout();
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
                .threadId(context.getThreadId())
                .workflowId(context.getWorkflowId())
                .taskId(context.getRunId())
                .event("workflow_started")
                .data(DifyData.builder()
                        .id(context.getRunId())
                        .workflowId(context.getWorkflowId())
                        .inputs(context.getUserInputs())
                        .createdAt(System.currentTimeMillis())
                        .build())
                .build();
        SseHelper.sendEvent(emitter, event);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
                .threadId(context.getThreadId())
                .workflowId(context.getWorkflowId())
                .taskId(context.getRunId())
                .event("workflow_finished")
                .data(DifyData.builder()
                        .id(context.getRunId())
                        .workflowId(context.getWorkflowId())
                        .inputs(context.getUserInputs())
                        .error(Objects.isNull(context.getWorkflowRunResult().getError())
                                ? null
                                : context.getWorkflowRunResult().getError().getMessage())
                        .outputs(context.getWorkflowRunResult().getOutputs())
                        .status(context.getWorkflowRunResult().getStatus().name())
                        .createdAt(System.currentTimeMillis())
                        .finishedAt(System.currentTimeMillis())
                        .elapsedTime(context.elapsedTime(LocalDateTime.now()) / 1000d)
                        .build())
                .build();
        SseHelper.sendEvent(emitter, event);
        emitter.complete();
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        suspendedTime = System.currentTimeMillis();
        waitResume(context);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        // no-op
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
                .threadId(context.getThreadId())
                .workflowId(context.getWorkflowId())
                .taskId(context.getRunId())
                .event("workflow_finished")
                .data(DifyData.builder()
                        .id(context.getRunId())
                        .status(WorkflowRunState.WorkflowRunStatus.failed.name())
                        .workflowId(context.getWorkflowId())
                        .inputs(context.getUserInputs())
                        .error(error)
                        .createdAt(System.currentTimeMillis())
                        .finishedAt(System.currentTimeMillis())
                        .elapsedTime(context.elapsedTime(LocalDateTime.now()) / 1000d)
                        .build())
                .build();
        SseHelper.sendEvent(emitter, event);
        emitter.complete();
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
        DifyEvent event = null;
        Node meta = context.getGraph().node(nodeId);
        if(meta.getNodeType().equals(NodeType.ITERATION.name)) {
            Object metadata = context.getState().getVariable(nodeId, "metadata");
            event = DifyEvent.builder()
                    .workflowRunId(context.getRunId())
                    .threadId(context.getThreadId())
                    .workflowId(context.getWorkflowId())
                    .taskId(context.getRunId())
                    .event("iteration_started")
                    .data(DifyData.builder()
                            .id(nodeRunId)
                            .nodeId(nodeId)
                            .type(meta.getNodeType())
                            .metadata(metadata)
                            .title(context.getNode(nodeId).getMeta().getTitle())
                            .createdAt(System.currentTimeMillis())
                            .build())
                    .build();
        } else {
            event = DifyEvent.builder()
                    .workflowRunId(context.getRunId())
                    .threadId(context.getThreadId())
                    .workflowId(context.getWorkflowId())
                    .taskId(context.getRunId())
                    .event("node_started")
                    .data(DifyData.builder()
                            .id(context.getRunId() + nodeId)
                            .nodeId(nodeId)
                            .type(meta.getNodeType())
                            .title(context.getNode(nodeId).getMeta().getTitle())
                            .createdAt(System.currentTimeMillis())
                            .build())
                    .build();
        }
        SseHelper.sendEvent(emitter, event);

    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData pdata) {
        if(ProgressData.ObjectType.DELTA_CONTENT.equals(pdata.getObject())) {
            DifyEvent event = DifyEvent.builder()
                    .id(((Delta) pdata.getData()).getMessageId())
                    .workflowRunId(context.getRunId())
                    .threadId(context.getThreadId())
                    .workflowId(context.getWorkflowId())
                    .taskId(context.getRunId())
                    .event("message")
                    .answer(pdata.getData().toString())
                    .build();
            SseHelper.sendEvent(emitter, event);
        }
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
        DifyEvent event = null;
        Object exeMetadata = null;
        if(context.getState().getNodeState(nodeId).getProcessData() != null) {
            exeMetadata = context.getState().getNodeState(nodeId).getProcessData().get("meta_data");
        }

        Node meta = context.getGraph().node(nodeId);
        if(meta.getNodeType().equals(NodeType.ITERATION.name)) {
            Object metadata = context.getState().getVariable(nodeId, "metadata");
            event = DifyEvent.builder()
                    .workflowRunId(context.getRunId())
                    .threadId(context.getThreadId())
                    .workflowId(context.getWorkflowId())
                    .taskId(context.getRunId())
                    .event("iteration_completed")
                    .data(DifyData.builder()
                            .id(context.getRunId() + nodeId)
                            .nodeId(nodeId)
                            .type(meta.getNodeType())
                            .title(context.getNode(nodeId).getMeta().getTitle())
                            .inputs(context.getState().getNodeState(nodeId).getInputs())
                            .outputs(context.getState().getNodeState(nodeId).getOutputs())
                            .processData(context.getState().getNodeState(nodeId).getProcessData())
                            .status(context.getState().getNodeState(nodeId).getStatus().name())
                            .executionMetadata(metadata)
                            .metadata(metadata)
                            .createdAt(System.currentTimeMillis())
                            .finishedAt(System.currentTimeMillis())
                            .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                            .build())
                    .build();
        } else {
            event = DifyEvent.builder()
                    .workflowRunId(context.getRunId())
                    .threadId(context.getThreadId())
                    .workflowId(context.getWorkflowId())
                    .taskId(context.getRunId())
                    .event("node_finished")
                    .data(DifyData.builder()
                            .id(context.getRunId() + nodeId)
                            .nodeId(nodeId)
                            .type(meta.getNodeType())
                            .title(context.getNode(nodeId).getMeta().getTitle())
                            .inputs(context.getState().getNodeState(nodeId).getInputs())
                            .outputs(context.getState().getNodeState(nodeId).getOutputs())
                            .processData(context.getState().getNodeState(nodeId).getProcessData())
                            .status(context.getState().getNodeState(nodeId).getStatus().name())
                            .executionMetadata(exeMetadata)
                            .createdAt(System.currentTimeMillis())
                            .finishedAt(System.currentTimeMillis())
                            .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                            .build())
                    .build();
        }

        SseHelper.sendEvent(emitter, event);
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
        DifyEvent event = null;
        Object exeMetadata = null;
        if(context.getState().getNodeState(nodeId).getProcessData() != null) {
            exeMetadata = context.getState().getNodeState(nodeId).getProcessData().get("meta_data");
        }
        Node meta = context.getGraph().node(nodeId);
        if(meta.getNodeType().equals(NodeType.ITERATION.name)) {
            Object metadata = context.getState().getVariable(nodeId, "metadata");
            event = DifyEvent.builder()
                    .workflowRunId(context.getRunId())
                    .threadId(context.getThreadId())
                    .workflowId(context.getWorkflowId())
                    .taskId(context.getRunId())
                    .event("iteration_completed")
                    .data(DifyData.builder()
                            .id(context.getRunId() + nodeId)
                            .nodeId(nodeId)
                            .type(meta.getNodeType())
                            .title(context.getNode(nodeId).getMeta().getTitle())
                            .inputs(context.getState().getNodeState(nodeId).getInputs())
                            .outputs(context.getState().getNodeState(nodeId).getOutputs())
                            .processData(context.getState().getNodeState(nodeId).getProcessData())
                            .status(context.getState().getNodeState(nodeId).getStatus().name())
                            .executionMetadata(metadata)
                            .error(error)
                            .metadata(metadata)
                            .createdAt(System.currentTimeMillis())
                            .finishedAt(System.currentTimeMillis())
                            .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                            .build())
                    .build();
        } else {
            event = DifyEvent.builder()
                    .workflowRunId(context.getRunId())
                    .threadId(context.getThreadId())
                    .workflowId(context.getWorkflowId())
                    .taskId(context.getRunId())
                    .event("node_finished")
                    .data(DifyData.builder()
                            .id(context.getRunId() + nodeId)
                            .nodeId(nodeId)
                            .type(context.getNode(nodeId).getMeta().getNodeType())
                            .title(context.getNode(nodeId).getMeta().getTitle())
                            .inputs(context.getState().getNodeState(nodeId).getInputs())
                            .outputs(context.getState().getNodeState(nodeId).getOutputs())
                            .processData(context.getState().getNodeState(nodeId).getProcessData())
                            .error(error)
                            .status(context.getState().getNodeState(nodeId).getStatus().name())
                            .executionMetadata(exeMetadata)
                            .createdAt(System.currentTimeMillis())
                            .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                            .build())
                    .build();
        }
        SseHelper.sendEvent(emitter, event);
    }

    @Override
    public void onWorkflowIterationStarted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
                .threadId(context.getThreadId())
                .workflowId(context.getWorkflowId())
                .taskId(context.getRunId())
                .event("iteration_next")
                .data(DifyData.builder()
                        .id(context.getRunId() + nodeId)
                        .index(index)
                        .nodeId(nodeId)
                        .createdAt(System.currentTimeMillis())
                        .finishedAt(System.currentTimeMillis())
                        .build())
                .build();
        SseHelper.sendEvent(emitter, event);
    }

    @Override
    public void onWorkflowIterationCompleted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
    }

    private void waitResume(WorkflowContext context) {
        if(System.currentTimeMillis() - suspendedTime >= timeout) {
            DifyEvent event = DifyEvent.builder()
                    .workflowRunId(context.getRunId())
                    .threadId(context.getThreadId())
                    .workflowId(context.getWorkflowId())
                    .taskId(context.getRunId())
                    .event("workflow_finished")
                    .data(DifyData.builder()
                            .id(context.getRunId())
                            .status(WorkflowRunState.WorkflowRunStatus.failed.name())
                            .workflowId(context.getWorkflowId())
                            .inputs(context.getUserInputs())
                            .error("wait workflow resume timeout.")
                            .createdAt(System.currentTimeMillis())
                            .finishedAt(System.currentTimeMillis())
                            .elapsedTime(context.elapsedTime(LocalDateTime.now()) / 1000d)
                            .build())
                    .build();
            SseHelper.sendEvent(emitter, event);
            emitter.complete();
        }
        TaskExecutor.schedule(() -> {
            if(!ws.tryResumeWorkflow(context, this)) {
                waitResume(context);
            }
        }, 5000);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifyEvent {
        private String event;
        @JsonProperty("workflow_run_id")
        private String workflowRunId;
        @JsonProperty("workflow_id")
        private String workflowId;
        @JsonProperty("task_id")
        private String taskId;
        private DifyData data;
        private String answer;
        private String id;
        @JsonProperty("thread_id")
        private String threadId;

        @JsonProperty("conversation_id")
        public String getConversationId() {
            return threadId;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class DifyData {
        private String id;
        @JsonProperty("node_id")
        private String nodeId;
        @JsonProperty("node_type")
        private String type;
        private String title;
        @JsonProperty("workflow_id")
        private String workflowId;
        @JsonProperty("execution_metadata")
        private Object executionMetadata;
        private Map inputs;
        private Map outputs;
        @JsonProperty("process_data")
        private Map processData;
        private String status;
        private String error;
        @JsonProperty("created_at")
        private Long createdAt;
        @JsonProperty("finished_at")
        private Long finishedAt;
        @JsonProperty("elapsed_time")
        private Double elapsedTime;
        private String text;
        private Object metadata;
        private Integer index;
    }

}
