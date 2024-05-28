package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;

public class WorkflowRunNotifyCallback extends WorkflowCallbackAdaptor {
    String notifyUrl;
    Map<String, Object> data = new LinkedHashMap<>();

    public WorkflowRunNotifyCallback(String notifyUrl) {
        this.notifyUrl = notifyUrl;
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
