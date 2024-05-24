package com.ke.bella.workflow.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.WorkflowSchema.Variable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("rawtypes")
public class TemplateTransformNode extends BaseNode {

    private static final int MAX_TEMPLATE_TRANSFORM_OUTPUT_LENGTH = 80000;

    private Data data;

    public TemplateTransformNode(Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map inputs = new LinkedHashMap<>();
        try {
            data.getVariables()
                    .forEach(v -> inputs.put(v.getVariable(),
                            context.getState().getVariableValue(v.getValueSelector())));

            String text = Variables.render(data.getTemplate(), inputs);
            Assert.isTrue(text.length() <= MAX_TEMPLATE_TRANSFORM_OUTPUT_LENGTH, "Output length exceeds");

            Map outputs = new LinkedHashMap();
            outputs.put("output", text);
            return NodeRunResult.builder()
                    .inputs(inputs)
                    .outputs(outputs)
                    .status(NodeRunResult.Status.succeeded)
                    .build();
        } catch (Exception e) {
            return NodeRunResult.builder()
                    .inputs(inputs)
                    .status(NodeRunResult.Status.failed)
                    .error(e)
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {
        List<Variable> variables = Collections.emptyList();
        String template;
    }
}
