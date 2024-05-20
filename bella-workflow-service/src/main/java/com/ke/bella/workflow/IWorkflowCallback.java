package com.ke.bella.workflow;

public interface IWorkflowCallback {
    void onWorkflowRunStarted(WorkflowContext context);

    void onWorkflowRunSucceeded(WorkflowContext context);

    void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t);

    void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t);
}
