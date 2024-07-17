package com.ke.bella.workflow.node;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;

public interface RunnableNode {
    NodeRunResult run(WorkflowContext context, IWorkflowCallback callback);
}
