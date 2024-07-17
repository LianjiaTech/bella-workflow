package com.ke.bella.workflow.service.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.ke.bella.workflow.service.IWorkflowCallback;
import com.ke.bella.workflow.service.JsonUtils;
import com.ke.bella.workflow.service.Variables;
import com.ke.bella.workflow.service.WorkflowContext;
import com.ke.bella.workflow.service.IWorkflowCallback.Delta;
import com.ke.bella.workflow.service.IWorkflowCallback.ProgressData;
import com.ke.bella.workflow.service.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.service.WorkflowSchema.Node;
import com.ke.bella.workflow.service.WorkflowSchema.Variable;

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
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map inputs = new LinkedHashMap<>();
        try {
            data.getVariables()
                    .forEach(v -> inputs.put(v.getVariable(),
                            context.getState().getVariableValue(v.getValueSelector())));

            String text = Variables.renderJinjia(data.getTemplate(), inputs);
            Assert.isTrue(text.length() <= MAX_TEMPLATE_TRANSFORM_OUTPUT_LENGTH, "Output length exceeds");

            if(data.isGenerateDeltaContent()) {
                Delta delta = Delta.builder().content(Delta.fromText(text)).build();
                callback.onWorkflowNodeRunProgress(context, getNodeId(), nodeRunId, ProgressData
                        .builder()
                        .data(delta)
                        .object(ProgressData.ObjectType.DELTA_CONTENT)
                        .build());
            }

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
