package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.service.WorkflowService;

public class SingleNodeRunBlockingCallback extends WorkflowRunBlockingCallback {
    Map<String, Object> data = new LinkedHashMap<>();

    public SingleNodeRunBlockingCallback(WorkflowService ws, long timeout) {
        super(ws, timeout);
    }

    public Object getWorkflowNodeRunResult() {
        return getWorkflowRunResult();
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeResult(context, data, nodeId);

    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeResult(context, data, nodeId);
    }
}
