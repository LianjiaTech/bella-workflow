package com.ke.bella.workflow;

import java.util.Map;

public class WorkflowCallbackAdaptor implements IWorkflowCallback {

    protected void responseWorkflowInfo(WorkflowContext context, Map<String, Object> data) {
        data.put("tenantId", context.getTenantId());
        data.put("workflowId", context.getWorkflowId());
        data.put("workflowRunId", context.getRunId());
        data.put("status", context.getState().getStatus().name());
    }

    protected void responseWorkflowMeta(WorkflowContext context, Map<String, Object> data) {
        data.put("meta", context.getGraph().getMeta());
    }

    protected void responseWorkflowOutputs(WorkflowContext context, Map<String, Object> data) {
        data.put("outputs", context.getWorkflowRunResult().getOutputs());
    }

    protected void responseWorkflowError(WorkflowContext context, Map<String, Object> data, String error) {
        data.put("error", error);
    }

    protected void responseWorkflowNodeInfo(WorkflowContext context, Map<String, Object> data, String nodeId) {
        data.put("nodeId", nodeId);
        data.put("nodeType", context.getGraph().node(nodeId).getNodeType());
    }

    protected void responseWorkflowNodeProgress(WorkflowContext context, Map<String, Object> data, ProgressData pd) {
        data.put("progress", pd);
    }

    protected void responseWorkflowNodeResult(WorkflowContext context, Map<String, Object> data, String nodeId) {
        data.put("result", context.getState().getNodeState(nodeId));
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData data) {
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
    }

    @Override
    public void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId, String nodeRunId) {
    }

    @Override
    public void onWorkflowIterationStarted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
    }

    @Override
    public void onWorkflowIterationCompleted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
    }

}
