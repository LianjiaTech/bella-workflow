package com.ke.bella.workflow.service;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;

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

        service.updateWorkflowRun(context, WorkflowRunStatus.running.name());

        delegate.onWorkflowRunStarted(context);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        LOGGER.info("{} onWorkflowRunSucceeded", context.getRunId());

        service.updateWorkflowRun(context, WorkflowRunStatus.succeeded.name(), context.getWorkflowRunResult().getOutputs());

        delegate.onWorkflowRunSucceeded(context);
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        LOGGER.info("{} onWorkflowRunSuspended", context.getRunId());

        service.updateWorkflowRun(context, WorkflowRunStatus.suspended.name());

        delegate.onWorkflowRunSuspended(context);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        LOGGER.info("{} onWorkflowRunResumed", context.getRunId());

        service.updateWorkflowRun(context, WorkflowRunStatus.running.name());

        delegate.onWorkflowRunResumed(context);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        LOGGER.info("{} onWorkflowRunFailed, error: {}", context.getRunId(), error, t);

        service.updateWorkflowRun(context, WorkflowRunStatus.failed.name(), error);

        delegate.onWorkflowRunFailed(context, error, t);
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId) {
        LOGGER.info("{} onWorkflowNodeRunStarted", context.getRunId());

        service.createWorkflowNodeRun(context, nodeId, NodeRunResult.Status.running.name());

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

        service.updateWorkflowNodeRunSucceeded(context, nodeId);

        delegate.onWorkflowNodeRunSucceeded(context, nodeId);

    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
        LOGGER.info("{} onWorkflowNodeRunFailed. error:{}", context.getRunId(), error, t);

        service.updateWorkflowNodeRunFailed(context, nodeId, error);

        delegate.onWorkflowNodeRunFailed(context, nodeId, error, t);
    }

}
