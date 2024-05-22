package com.ke.bella.workflow;

public class WorkflowCallbackAdaptor implements IWorkflowCallback {

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId) {
    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId) {
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
    }

}
