package com.ke.bella.workflow.service;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowRunCallback implements IWorkflowCallback {

    private final WorkflowService service;
    private final IWorkflowCallback delegate;

    public WorkflowRunCallback(WorkflowService service, IWorkflowCallback delegate) {
        this.service = service;
        this.delegate = delegate;
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
        LOGGER.info("{} onWorkflowRunStarted", context.getRunId());
        delegate.onWorkflowRunStarted(context);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        LOGGER.info("{} onWorkflowRunSucceeded", context.getRunId());
        delegate.onWorkflowRunSucceeded(context);
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        LOGGER.info("{} onWorkflowRunSuspended", context.getRunId());
        delegate.onWorkflowRunSuspended(context);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        LOGGER.info("{} onWorkflowRunResumed", context.getRunId());
        delegate.onWorkflowRunResumed(context);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        LOGGER.info("{} onWorkflowRunFailed", context.getRunId());
        delegate.onWorkflowRunFailed(context, error, t);
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId) {
        LOGGER.info("{} onWorkflowNodeRunStarted", context.getRunId());
        delegate.onWorkflowNodeRunStarted(context, nodeId);
    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId) {
        LOGGER.info("{} onWorkflowNodeRunProgress", context.getRunId());
        delegate.onWorkflowNodeRunProgress(context, nodeId);
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
        LOGGER.info("{} onWorkflowNodeRunSucceeded", context.getRunId());
        delegate.onWorkflowNodeRunSucceeded(context, nodeId);

    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
        LOGGER.info("{} onWorkflowNodeRunFailed", context.getRunId());
        delegate.onWorkflowNodeRunFailed(context, nodeId, error, t);
    }

}
