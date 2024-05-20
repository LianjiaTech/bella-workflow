package com.ke.bella.workflow.node;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;

public interface RunnableNode {
    void run(WorkflowContext context, IWorkflowCallback callback);
}
