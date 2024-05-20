package com.ke.bella.workflow;

import java.util.List;

import com.ke.bella.workflow.node.BaseNode;

public class WorkflowRunner {

    public void run(WorkflowContext context, IWorkflowCallback callback) {
        try {
            callback.onWorkflowRunStarted(context);
            context.validate();

            List<BaseNode> nodes = context.getNextNodes();
            while (!nodes.isEmpty()) {
                for (BaseNode node : nodes) {
                    node.run(context, callback);
                }
                nodes = context.getNextNodes();
            }

            callback.onWorkflowRunSucceeded(context);
        } catch (Exception e) {
            callback.onWorkflowRunFailed(context, e.getMessage(), e);
        }
    }
}