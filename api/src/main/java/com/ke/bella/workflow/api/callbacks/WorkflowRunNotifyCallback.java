package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.utils.HttpUtils;

public class WorkflowRunNotifyCallback extends WorkflowCallbackAdaptor {
    Map<String, Object> data = new LinkedHashMap<>();

    final WorkflowService ws;
    final String url;

    public WorkflowRunNotifyCallback(WorkflowService ws, String url) {
        this.ws = ws;
        this.url = url;
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        synchronized(data) {
            responseWorkflowInfo(context, data);
            responseWorkflowOutputs(context, data);
        }
        TaskExecutor.submit(this::notifyClient);

    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        synchronized(data) {
            responseWorkflowInfo(context, data);
            responseWorkflowError(context, data, error);
        }
        TaskExecutor.submit(this::notifyClient);
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
    }

    private void notifyClient() {
        int code = HttpUtils.postJson(url, data);
        if(code >= 200 && code <= 299) {
            ws.markWorkflowRunCallbacked((String) data.get("workflowRunId"));
        }
    }
}
