package com.ke.bella.workflow.api.callbacks;

import com.ke.bella.workflow.service.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.service.WorkflowContext;

public class DifySingleNodeRunBlockingCallback extends WorkflowCallbackAdaptor {
    DifyWorkflowRunStreamingCallback.DifyData data = DifyWorkflowRunStreamingCallback.DifyData.builder().build();

    public Object getWorkflowNodeRunResult() {
        return data;
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {

        data = DifyWorkflowRunStreamingCallback.DifyData.builder()
                .id(context.getRunId() + nodeId)
                .nodeId(nodeId)
                .title(context.getNode(nodeId).getMeta().getTitle())
                .inputs(context.getState().getNodeState(nodeId).getInputs())
                .outputs(context.getState().getNodeState(nodeId).getOutputs())
                .error(null)
                .status(context.getState().getNodeState(nodeId).getStatus().name())
                .executionMetadata(null)
                .createdAt(System.currentTimeMillis())
                .finishedAt(System.currentTimeMillis())
                .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                .build();

    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {

        data = DifyWorkflowRunStreamingCallback.DifyData.builder()
                .id(context.getRunId() + nodeId)
                .nodeId(nodeId)
                .title(context.getNode(nodeId).getMeta().getTitle())
                .inputs(context.getState().getNodeState(nodeId).getInputs())
                .error(error)
                .status(context.getState().getNodeState(nodeId).getStatus().name())
                .executionMetadata(null)
                .createdAt(System.currentTimeMillis())
                .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                .build();

    }

}
