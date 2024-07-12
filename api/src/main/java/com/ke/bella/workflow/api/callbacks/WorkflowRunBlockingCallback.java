package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ke.bella.workflow.service.TaskExecutor;
import com.ke.bella.workflow.service.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.service.WorkflowContext;
import com.ke.bella.workflow.service.WorkflowService;

public class WorkflowRunBlockingCallback extends WorkflowCallbackAdaptor {
    Map<String, Object> data = new LinkedHashMap<>();

    final WorkflowService ws;
    final long timeout;
    long suspendedTime = System.currentTimeMillis();

    public WorkflowRunBlockingCallback(WorkflowService ws, long timeout) {
        this.ws = ws;
        this.timeout = timeout;
    }

    public Map<String, Object> getWorkflowRunResult() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            synchronized(data) {
                if(!data.isEmpty()) {
                    return data;
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return data;
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        synchronized(data) {
            responseWorkflowInfo(context, data);
            responseWorkflowOutputs(context, data);
        }

    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        synchronized(data) {
            responseWorkflowInfo(context, data);
            responseWorkflowError(context, data, error);
        }
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);

        suspendedTime = System.currentTimeMillis();
        waitResume(context);
    }

    private void waitResume(WorkflowContext context) {
        if(System.currentTimeMillis() - suspendedTime < timeout) {
            TaskExecutor.schedule(() -> {
                if(!ws.tryResumeWorkflow(context, this)) {
                    waitResume(context);
                }
            }, 5000);
        }
    }
}
