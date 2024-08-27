package com.ke.bella.workflow.node;

import java.util.Arrays;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.node.BaseNode.BaseNodeData;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ParallelNode extends BaseNode<ParallelNode.Data> {

    public ParallelNode(Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
    }

    @Override
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        try {
            return NodeRunResult.builder()
                    .status(NodeRunResult.Status.succeeded)
                    .activatedSourceHandles(data.getSourceHandles())
                    .build();
        } catch (Exception e) {
            return NodeRunResult.builder()
                    .error(e)
                    .status(NodeRunResult.Status.failed)
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {
    }
}
