package com.ke.bella.workflow.api.callbacks;

import java.util.Map;
import java.util.Objects;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.api.SseHelper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DifyWorkflowRunStreamingCallback extends WorkflowCallbackAdaptor {

    final SseEmitter emitter;

    public DifyWorkflowRunStreamingCallback(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
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
                        .elapsedTime(context.getWorkflowRunResult().getElapsedTime() / 1000d)
                        .build())
                .build();
        SseHelper.sendEvent(emitter, event);
        emitter.complete();
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        // no-op
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        // no-op
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
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
                        .elapsedTime(0.1)
                        .build())
                .build();
        SseHelper.sendEvent(emitter, event);
        emitter.complete();
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
                .workflowId(context.getWorkflowId())
                .taskId(context.getRunId())
                .event("node_started")
                .data(DifyData.builder()
                        .id(context.getRunId() + nodeId)
                        .nodeId(nodeId)
                        .title(context.getNode(nodeId).getMeta().getTitle())
                        .createdAt(System.currentTimeMillis())
                        .build())
                .build();
        SseHelper.sendEvent(emitter, event);

    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData pdata) {
        if(ProgressData.ObjectType.DELTA_CONTENT.equals(pdata.getObject())) {
            DifyEvent event = DifyEvent.builder()
                    .workflowRunId(context.getRunId())
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
        Object metadata = null;
        if(context.getState().getNodeState(nodeId).getProcessData() != null) {
            metadata = context.getState().getNodeState(nodeId).getProcessData().get("meta_data");
        }
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
                .workflowId(context.getWorkflowId())
                .taskId(context.getRunId())
                .event("node_finished")
                .data(DifyData.builder()
                        .id(context.getRunId() + nodeId)
                        .nodeId(nodeId)
                        .title(context.getNode(nodeId).getMeta().getTitle())
                        .inputs(context.getState().getNodeState(nodeId).getInputs())
                        .outputs(context.getState().getNodeState(nodeId).getOutputs())
                        .processData(context.getState().getNodeState(nodeId).getProcessData())
                        .status(context.getState().getNodeState(nodeId).getStatus().name())
                        .executionMetadata(metadata)
                        .createdAt(System.currentTimeMillis())
                        .finishedAt(System.currentTimeMillis())
                        .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                        .build())
                .build();

        SseHelper.sendEvent(emitter, event);
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
        Object metadata = null;
        if(context.getState().getNodeState(nodeId).getProcessData() != null) {
            metadata = context.getState().getNodeState(nodeId).getProcessData().get("meta_data");
        }
        DifyEvent event = DifyEvent.builder()
                .workflowRunId(context.getRunId())
                .workflowId(context.getWorkflowId())
                .taskId(context.getRunId())
                .event("node_finished")
                .data(DifyData.builder()
                        .id(context.getRunId() + nodeId)
                        .nodeId(nodeId)
                        .title(context.getNode(nodeId).getMeta().getTitle())
                        .inputs(context.getState().getNodeState(nodeId).getInputs())
                        .outputs(context.getState().getNodeState(nodeId).getOutputs())
                        .processData(context.getState().getNodeState(nodeId).getProcessData())
                        .error(error)
                        .status(context.getState().getNodeState(nodeId).getStatus().name())
                        .executionMetadata(metadata)
                        .createdAt(System.currentTimeMillis())
                        .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                        .build())
                .build();
        SseHelper.sendEvent(emitter, event);
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
    }

}
