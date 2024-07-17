package com.ke.bella.workflow.service.node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ke.bella.workflow.service.IWorkflowCallback;
import com.ke.bella.workflow.service.JsonUtils;
import com.ke.bella.workflow.service.WorkflowContext;
import com.ke.bella.workflow.service.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.service.WorkflowSchema;
import com.ke.bella.workflow.service.WorkflowSchema.Node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("rawtypes")
public class End extends BaseNode {

    private Data data;

    public End(Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map outputs = new LinkedHashMap<>();
        data.getOutputs()
                .forEach(v -> outputs.put(v.getVariable(), context.getState().getVariableValue(v.getValueSelector())));
        return NodeRunResult.builder()
                .outputs(outputs)
                .status(NodeRunResult.Status.succeeded)
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {
        List<WorkflowSchema.Variable> outputs;
    }
}
