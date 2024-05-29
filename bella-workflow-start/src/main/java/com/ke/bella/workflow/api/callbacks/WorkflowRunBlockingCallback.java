package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;

public class WorkflowRunBlockingCallback extends WorkflowCallbackAdaptor {
    Map<String, Object> data = new LinkedHashMap<>();

    public Map<String, Object> getWorkflowRunResult(long timeout) {
        return data;
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        responseWorkflowInfo(context, data);
        responseWorkflowOutputs(context, data);
        responseWorkflowMeta(context, data);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        responseWorkflowInfo(context, data);
        responseWorkflowError(context, data, error);
    }
}
