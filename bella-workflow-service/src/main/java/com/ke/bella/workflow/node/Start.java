package com.ke.bella.workflow.node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.WorkflowSchema.VariableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("rawtypes")
public class Start extends BaseNode {

    private Data data;

    public Start(Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
    }

    @Override
    public NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map workflowInputs = context.userInputs();
        Map nodeInputs = validateAndExtract(workflowInputs);
        return NodeRunResult.builder()
                .inputs(nodeInputs)
                .status(NodeRunResult.Status.succeeded)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map validateAndExtract(Map workflowInputs) {
        Map nodeInputs = new LinkedHashMap();
        data.getVariables()
                .forEach(v -> nodeInputs.put(v.getVariable(), workflowInputs.get(v.getVariable())));
        return nodeInputs;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {
        List<VariableEntity> variables;
    }
}
