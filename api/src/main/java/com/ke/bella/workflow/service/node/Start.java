package com.ke.bella.workflow.service.node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ke.bella.workflow.service.IWorkflowCallback;
import com.ke.bella.workflow.service.JsonUtils;
import com.ke.bella.workflow.service.WorkflowContext;
import com.ke.bella.workflow.service.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.service.WorkflowSchema.Node;
import com.ke.bella.workflow.service.WorkflowSchema.VariableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("rawtypes")
@Getter
public class Start extends BaseNode {

    private Data data;

    public Start(Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
    }

    @Override
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
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
