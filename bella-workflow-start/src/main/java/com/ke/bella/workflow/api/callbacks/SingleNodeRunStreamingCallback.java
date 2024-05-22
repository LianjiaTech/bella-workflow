package com.ke.bella.workflow.api.callbacks;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.api.SseHelper;

public class SingleNodeRunStreamingCallback extends WorkflowCallbackAdaptor {
    final SseEmitter emitter;

    public SingleNodeRunStreamingCallback(SseEmitter emitter) {
        this.emitter = emitter;
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
