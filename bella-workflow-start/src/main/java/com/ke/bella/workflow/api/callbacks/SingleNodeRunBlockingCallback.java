package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;

public class SingleNodeRunBlockingCallback extends WorkflowCallbackAdaptor {
    Map<String, Object> data = new LinkedHashMap<>();

    public Object getWorkflowNodeRunResult(String nodeId, long l) {
        return data;
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeResult(context, data, nodeId);

    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
        responseWorkflowInfo(context, data);
        responseWorkflowNodeInfo(context, data, nodeId);
        responseWorkflowNodeResult(context, data, nodeId);

    }

}
