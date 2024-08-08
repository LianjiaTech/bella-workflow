package com.ke.bella.workflow.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.IWorkflowCallback.Delta;
import com.ke.bella.workflow.IWorkflowCallback.ProgressData;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.WorkflowSchema.Variable;
import com.ke.bella.workflow.service.code.CodeExecutor;
import com.ke.bella.workflow.utils.JsonUtils;

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
            Map codeResult = CodeExecutor.execute(CodeExecutor.CodeLanguage.jinja2, data.getTemplate(), inputs, null);

            String text = codeResult.get("result").toString();
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

    public static Map<String, Object> defaultConfig(Map<String, Object> filters) {
        return JsonUtils.fromJson(
                "{\"type\":\"template-transform\",\"config\":{\"variables\":[{\"variable\":\"arg1\",\"value_selector\":[]}],\"template\":\"{{ arg1 }}\"}}",
                new TypeReference<Map<String, Object>>() {
                });
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {
        List<Variable> variables = Collections.emptyList();
        String template;
    }
}
