package com.ke.bella.workflow.service;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;

public class WorkflowRunCallback implements IWorkflowCallback {

    private final WorkflowService service;
    private final IWorkflowCallback delegate;

    public WorkflowRunCallback(WorkflowService service, IWorkflowCallback delegate) {
        this.service = service;
        this.delegate = delegate;
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
        delegate.onWorkflowRunStarted(context);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        delegate.onWorkflowRunSucceeded(context);
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        delegate.onWorkflowRunSuspended(context);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        delegate.onWorkflowRunResumed(context);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        delegate.onWorkflowRunFailed(context, error, t);
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId) {
        delegate.onWorkflowNodeRunStarted(context, nodeId);
    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId) {
        delegate.onWorkflowNodeRunProgress(context, nodeId);
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
        delegate.onWorkflowNodeRunSucceeded(context, nodeId);

    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
        delegate.onWorkflowNodeRunFailed(context, nodeId, error, t);
    }

}
