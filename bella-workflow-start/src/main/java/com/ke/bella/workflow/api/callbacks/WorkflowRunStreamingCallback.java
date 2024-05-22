package com.ke.bella.workflow.api.callbacks;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.api.SseHelper;

public class WorkflowRunStreamingCallback implements IWorkflowCallback {

    final SseEmitter emitter;

    public WorkflowRunStreamingCallback(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
        // TODO
        SseHelper.sendEvent(emitter, "onWorkflowRunStarted", null);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        // TODO Auto-generated method stub
        SseHelper.sendEvent(emitter, "onWorkflowRunSucceeded", null);

    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        // TODO Auto-generated method stub
        SseHelper.sendEvent(emitter, "onWorkflowRunSuspended", null);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        // TODO Auto-generated method stub
        SseHelper.sendEvent(emitter, "onWorkflowRunResumed", null);

    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        // TODO Auto-generated method stub
        SseHelper.sendEvent(emitter, "onWorkflowRunFailed", null);
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId) {
        // TODO Auto-generated method stub
        SseHelper.sendEvent(emitter, "onWorkflowNodeRunStarted", null);

    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId) {
        // TODO Auto-generated method stub
        SseHelper.sendEvent(emitter, "onWorkflowNodeRunProgress", null);

    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
        // TODO Auto-generated method stub
        SseHelper.sendEvent(emitter, "onWorkflowNodeRunSucceeded", null);
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
        // TODO Auto-generated method stub
        SseHelper.sendEvent(emitter, "onWorkflowNodeRunFailed", null);
    }

}
