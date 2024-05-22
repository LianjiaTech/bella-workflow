package com.ke.bella.workflow.api.callbacks;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;

public class WorkflowRunBlockingCallback extends WorkflowCallbackAdaptor {

    public Object getWorkflowRunResult(long timeout) {
        return "{}"; // TODO
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
    }
}
