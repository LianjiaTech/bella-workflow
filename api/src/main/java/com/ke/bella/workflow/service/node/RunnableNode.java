package com.ke.bella.workflow.service.node;

import com.ke.bella.workflow.service.IWorkflowCallback;
import com.ke.bella.workflow.service.WorkflowContext;
import com.ke.bella.workflow.service.WorkflowRunState.NodeRunResult;

public interface RunnableNode {
    NodeRunResult run(WorkflowContext context, IWorkflowCallback callback);
}
