package com.ke.bella.workflow.api.callbacks;

import java.util.concurrent.TimeUnit;

import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.service.WorkflowService;

public class DifySingleNodeRunBlockingCallback extends WorkflowRunBlockingCallback {

    DifyWorkflowRunStreamingCallback.DifyData data;

    String waitingNodeId;

    public DifySingleNodeRunBlockingCallback(WorkflowService ws, long timeout) {
        super(ws, timeout);
    }

    public Object getWorkflowNodeRunResult() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            if(data != null) {
                return data;
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
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {

        data = DifyWorkflowRunStreamingCallback.DifyData.builder()
                .id(context.getRunId() + nodeId)
                .nodeId(nodeId)
                .title(context.getNodeMeta(nodeId).getTitle())
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
                .title(context.getNodeMeta(nodeId).getTitle())
                .inputs(context.getState().getNodeState(nodeId).getInputs())
                .error(error)
                .status(context.getState().getNodeState(nodeId).getStatus().name())
                .executionMetadata(null)
                .createdAt(System.currentTimeMillis())
                .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                .build();

    }

    @Override
    public void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId, String nodeRunId) {
        waitingNodeId = nodeId;
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        suspendedTime = System.currentTimeMillis();
        waitResume(context, waitingNodeId);
    }

    private void waitResume(WorkflowContext context, String nodeId) {
        if(System.currentTimeMillis() - suspendedTime >= timeout) {
            data = DifyWorkflowRunStreamingCallback.DifyData.builder()
                    .id(context.getRunId() + nodeId)
                    .nodeId(nodeId)
                    .title(context.getNodeMeta(nodeId).getTitle())
                    .inputs(context.getState().getNodeState(nodeId).getInputs())
                    .error("wait workflow resume timeout.")
                    .status(context.getState().getNodeState(nodeId).getStatus().name())
                    .executionMetadata(null)
                    .createdAt(System.currentTimeMillis())
                    .elapsedTime(context.getState().getNodeState(nodeId).getElapsedTime() / 1000d)
                    .build();
        }

        if(!ws.tryResumeWorkflow(context, this)) {
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                throw new IllegalStateException("waitResume interrupted", e);
            }
            waitResume(context, nodeId);
        }
    }
}
