package com.ke.bella.workflow;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

public interface IWorkflowCallback {

    @Getter
    @Setter
    @SuperBuilder
    public static class ProgressData {
        int progress;
        Object data;
    }

    void onWorkflowRunStarted(WorkflowContext context);

    void onWorkflowRunSucceeded(WorkflowContext context);

    void onWorkflowRunSuspended(WorkflowContext context);

    void onWorkflowRunResumed(WorkflowContext context);

    void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t);

    void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, ProgressData data);

    void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t);
}
