package com.ke.bella.workflow.api.callbacks;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;

public class WorkflowRunNotifyCallback extends WorkflowCallbackAdaptor {
    String notifyUrl;

    public WorkflowRunNotifyCallback(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        // TODO
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        // TODO
    }
}
