package com.ke.bella.workflow.service;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowRunCallback extends WorkflowCallbackAdaptor {

    private final WorkflowService service;
    private final IWorkflowCallback delegate;

    public WorkflowRunCallback(WorkflowService service, IWorkflowCallback delegate) {
        this.service = service;
        this.delegate = delegate;
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
        LOGGER.info("{} {} onWorkflowRunStarted", context.getWorkflowId(), context.getRunId());

        service.updateWorkflowRun(context, WorkflowRunStatus.running.name());

        delegate.onWorkflowRunStarted(context);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        LOGGER.info("{} {} onWorkflowRunSucceeded", context.getWorkflowId(), context.getRunId());

        service.updateWorkflowRun(context, WorkflowRunStatus.succeeded.name(), context.getWorkflowRunResult().getOutputs());

        delegate.onWorkflowRunSucceeded(context);
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        LOGGER.info("{} {} onWorkflowRunSuspended", context.getWorkflowId(), context.getRunId());

        service.updateWorkflowRun(context, WorkflowRunStatus.suspended.name());

        delegate.onWorkflowRunSuspended(context);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        LOGGER.info("{} {} onWorkflowRunResumed", context.getWorkflowId(), context.getRunId());

        service.updateWorkflowRun(context, WorkflowRunStatus.running.name());

        delegate.onWorkflowRunResumed(context);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        LOGGER.warn("{} {} onWorkflowRunFailed, error: {}", context.getWorkflowId(), context.getRunId(), error, t);

        service.updateWorkflowRun(context, WorkflowRunStatus.failed.name(), error);

        delegate.onWorkflowRunFailed(context, error, t);
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
        LOGGER.info("{} {} onWorkflowNodeRunStarted", context.getWorkflowId(), context.getRunId());

        if(!context.isFlashMode()) {
            service.createWorkflowNodeRun(context, nodeId, nodeRunId, NodeRunResult.Status.running.name());
        }

        delegate.onWorkflowNodeRunStarted(context, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData data) {
        LOGGER.info("{} {} onWorkflowNodeRunProgress", context.getWorkflowId(), context.getRunId());
        delegate.onWorkflowNodeRunProgress(context, nodeId, nodeRunId, data);
    }

    @Override
    public void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId, String nodeRunId) {
        LOGGER.info("{} {} onWorkflowNodeRunWaited", context.getWorkflowId(), context.getRunId());

        if(!context.isFlashMode()) {
            service.updateWorkflowNodeRunWaited(context, nodeId, nodeRunId);
        }

        delegate.onWorkflowNodeRunWaited(context, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
        LOGGER.info("{} {} onWorkflowNodeRunSucceeded", context.getWorkflowId(), context.getRunId());

        if(!context.isFlashMode()) {
            service.updateWorkflowNodeRunSucceeded(context, nodeId, nodeRunId);
        }

        delegate.onWorkflowNodeRunSucceeded(context, nodeId, nodeRunId);

    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
        LOGGER.info("{} {} onWorkflowNodeRunFailed. error:{}", context.getWorkflowId(), context.getRunId(), error, t);

        if(!context.isFlashMode()) {
            service.updateWorkflowNodeRunFailed(context, nodeId, nodeRunId, error);
        }

        delegate.onWorkflowNodeRunFailed(context, nodeId, nodeRunId, error, t);
    }

    @Override
    public void onWorkflowIterationStarted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        LOGGER.info("{} {} onWorkflowIterationStarted, index: {}", context.getWorkflowId(), context.getRunId(), index);
        delegate.onWorkflowIterationStarted(context, nodeId, nodeRunId, index);
    }

    @Override
    public void onWorkflowIterationCompleted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        LOGGER.info("{} {} onWorkflowIterationCompleted, index: {}", context.getWorkflowId(), context.getRunId(), index);
        delegate.onWorkflowIterationCompleted(context, nodeId, nodeRunId, index);
    }
}
