package com.ke.bella.workflow.service;

import java.util.List;

import com.ke.bella.workflow.service.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.service.WorkflowRunState.WorkflowRunStatus;
import com.ke.bella.workflow.service.node.BaseNode;

public class WorkflowRunner {

    public void run(WorkflowContext context, IWorkflowCallback callback) {
        context.getState().setStatus(WorkflowRunStatus.running);
        callback.onWorkflowRunStarted(context);
        run0(context, callback, context.getNextNodes());
    }

    public void runNode(WorkflowContext context, IWorkflowCallback callback, String nodeId) {
        BaseNode node = context.getNode(nodeId);
        node.run(context, callback);
    }

    public void resume(WorkflowContext context, IWorkflowCallback callback, List<String> nodeIds) {
        context.getState().setStatus(WorkflowRunStatus.running);
        callback.onWorkflowRunResumed(context);


        List<BaseNode> nodes = context.getNodes(nodeIds);
        run0(context, callback, nodes);
    }

    private void run0(WorkflowContext context, IWorkflowCallback callback, List<BaseNode> nodes) {
        try {
            context.validate();
            NodeRunResult result = null;
            while (!nodes.isEmpty()) {
                for (BaseNode node : nodes) {
                    if(context.isResume(node.getNodeId())) {
                        result = node.resume(context, callback);
                    } else {
                        result = node.run(context, callback);
                    }
                }
                nodes = context.getNextNodes();
            }

            if(context.isSuspended()) {
                context.getState().setStatus(WorkflowRunStatus.suspended);
                callback.onWorkflowRunSuspended(context);
            } else {
                context.putWorkflowRunResult(result);
                context.getState().setStatus(WorkflowRunStatus.succeeded);
                callback.onWorkflowRunSucceeded(context);
            }
        } catch (Exception e) {
            context.getState().setStatus(WorkflowRunStatus.failed);
            callback.onWorkflowRunFailed(context, e.getMessage(), e);
        }
    }
}
