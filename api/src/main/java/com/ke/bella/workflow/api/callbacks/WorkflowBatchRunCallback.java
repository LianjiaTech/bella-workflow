package com.ke.bella.workflow.api.callbacks;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.ke.bella.job.queue.worker.Task;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.utils.JsonUtils;

public class WorkflowBatchRunCallback extends WorkflowCallbackAdaptor {
    final Map<String, Object> data = new LinkedHashMap<>();

    private final Task task;

    public WorkflowBatchRunCallback(Task task) {
        this.task = task;
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        synchronized(data) {
            responseWorkflowInfo(context, data);
            responseWorkflowOutputs(context, data);
        }
        task.markSucceed(JsonUtils.toJson(data.get("outputs")));
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        synchronized(data) {
            responseWorkflowInfo(context, data);
            responseWorkflowError(context, data, error);
        }
        task.markFailed(MapUtils.getString(data, "error", StringUtils.EMPTY));
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
    }
}
