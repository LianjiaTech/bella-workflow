package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

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
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);

        SseHelper.sendEvent(emitter, "onWorkflowNodeRunStarted", data);

    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, ProgressData pdata) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeProgress(context, data, pdata);

        SseHelper.sendEvent(emitter, "onWorkflowNodeRunProgress", data);

    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeResult(context, data, nodeId);

        SseHelper.sendEvent(emitter, "onWorkflowNodeRunSucceeded", data);
        emitter.complete();
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeResult(context, data, nodeId);

        SseHelper.sendEvent(emitter, "onWorkflowNodeRunFailed", data);
        emitter.complete();
    }
}
