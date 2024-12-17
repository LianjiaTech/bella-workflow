package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.api.SseHelper;
import com.ke.bella.workflow.service.WorkflowService;

public class WorkflowRunStreamingCallback extends WorkflowCallbackAdaptor {

    final SseEmitter emitter;
    final WorkflowService ws;
    final long timeout;

    public WorkflowRunStreamingCallback(SseEmitter emitter, WorkflowService ws) {
        this.emitter = emitter;
        this.ws = ws;
        this.timeout = emitter.getTimeout();
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowMeta(context, data);

        SseHelper.sendEvent(emitter, "onWorkflowRunStarted", data);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowOutputs(context, data);

        SseHelper.sendEvent(emitter, "onWorkflowRunSucceeded", data);
        emitter.complete();

    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);

        SseHelper.sendEvent(emitter, "onWorkflowRunSuspended", data);

        ws.waitWorkflowRunNotify(context, this, timeout);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);

        SseHelper.sendEvent(emitter, "onWorkflowRunResumed", data);

    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowError(context, data, error);

        SseHelper.sendEvent(emitter, "onWorkflowRunFailed", data);
        emitter.complete();
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);

        SseHelper.sendEvent(emitter, "onWorkflowNodeRunStarted", data);

    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData pdata) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeProgress(context, data, pdata);

        SseHelper.sendEvent(emitter, "onWorkflowNodeRunProgress", data);

    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeResult(context, data, nodeId);

        SseHelper.sendEvent(emitter, "onWorkflowNodeRunSucceeded", data);
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeResult(context, data, nodeId);

        SseHelper.sendEvent(emitter, "onWorkflowNodeRunFailed", data);
    }
}
