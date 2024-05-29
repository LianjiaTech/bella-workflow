package com.ke.bella.workflow.api.callbacks;

import java.util.Map;

import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.utils.HttpUtils;

public class WorkflowRunNotifier implements Runnable {

    final String url;
    final Map<String, Object> data;
    WorkflowService ws;

    public WorkflowRunNotifier(String url, Map<String, Object> data, WorkflowService ws) {
        this.url = url;
        this.data = data;
        this.ws = ws;
    }

    @Override
    public void run() {
        int code = HttpUtils.postJson(url, data);
        if(code >= 200 && code <= 299) {
            ws.markWorkflowRunCallbacked((String) data.get("workflowRunId"));
        }
    }
}
