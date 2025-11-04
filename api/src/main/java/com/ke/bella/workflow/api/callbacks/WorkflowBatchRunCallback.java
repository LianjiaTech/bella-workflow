package com.ke.bella.workflow.api.callbacks;

import com.ke.bella.queue.TaskWrapper;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkflowBatchRunCallback extends WorkflowCallbackAdaptor {
    final Map<String, Object> data = new LinkedHashMap<>();

    private final TaskWrapper task;

    public WorkflowBatchRunCallback(TaskWrapper task) {
        this.task = task;
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        synchronized(data) {
            responseWorkflowInfo(context, data);
            responseWorkflowOutputs(context, data);
            responseWorkflowMetaData(context, data);
        }
        Object outputs = data.get("outputs");
        Map<String, Object> result = new HashMap<>();
        result.put("status_code", 200);
        result.put("request_id", task.getTask().getTaskId());
        result.put("body", outputs);
        task.markComplete(result);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        synchronized(data) {
            responseWorkflowInfo(context, data);
            responseWorkflowError(context, data, error);
        }
        String errorBody = MapUtils.getString(data, "error", StringUtils.EMPTY);
        Map<String, Object> result = new HashMap<>();
        result.put("status_code", 500);
        result.put("request_id", task.getTask().getTaskId());
        result.put("body", errorBody);
        task.markComplete(result);
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        responseWorkflowInfo(context, data);
    }
}
