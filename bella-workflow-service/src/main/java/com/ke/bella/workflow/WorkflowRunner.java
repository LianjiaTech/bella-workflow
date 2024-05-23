package com.ke.bella.workflow;

import java.util.Arrays;
import java.util.List;

import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.node.BaseNode;

public class WorkflowRunner {

    public void run(WorkflowContext context, IWorkflowCallback callback) {
        callback.onWorkflowRunStarted(context);
        run0(context, callback, context.getNextNodes());
    }

    public void runNode(WorkflowContext context, IWorkflowCallback callback, String nodeId) {
        BaseNode node = context.getNode(nodeId);
        node.run(context, callback);
    }

    public void resume(WorkflowContext context, IWorkflowCallback callback, String nodeId) {
        BaseNode node = context.getNode(nodeId);
        run0(context, callback, Arrays.asList(node));
    }

    private void run0(WorkflowContext context, IWorkflowCallback callback, List<BaseNode> nodes) {
        try {
            context.validate();
            NodeRunResult result = null;
            while (!nodes.isEmpty()) {
                for (BaseNode node : nodes) {
                    result = node.run(context, callback);
                }
                nodes = context.getNextNodes();
            }

            if(context.isSuspended()) {
                callback.onWorkflowRunSuspended(context);
            } else {
                context.putWorkflowRunResult(result);
                callback.onWorkflowRunSucceeded(context);
            }
        } catch (Exception e) {
            callback.onWorkflowRunFailed(context, e.getMessage(), e);
        }
    }
}
