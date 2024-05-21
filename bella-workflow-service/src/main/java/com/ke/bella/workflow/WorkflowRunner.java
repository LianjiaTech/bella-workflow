package com.ke.bella.workflow;

import java.util.List;

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

    public void resume(WorkflowContext context, IWorkflowCallback callback) {
        // TODO
    }

    private void run0(WorkflowContext context, IWorkflowCallback callback, List<BaseNode> nodes) {
        try {
            context.validate();
            while (!nodes.isEmpty()) {
                for (BaseNode node : nodes) {
                    node.run(context, callback);
                }
                nodes = context.getNextNodes();
            }

            if(context.isSuspended()) {
                callback.onWorkflowRunSuspended(context);
            } else {
                callback.onWorkflowRunSucceeded(context);
            }
        } catch (Exception e) {
            callback.onWorkflowRunFailed(context, e.getMessage(), e);
        }
    }
}
