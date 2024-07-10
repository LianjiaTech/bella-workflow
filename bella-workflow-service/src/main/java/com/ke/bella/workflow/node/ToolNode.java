package com.ke.bella.workflow.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.tool.ApiTool;
import com.ke.bella.workflow.tool.ToolManager;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ToolNode extends BaseNode {

    private final Data data;

    public ToolNode(WorkflowSchema.Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected WorkflowRunState.NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map<String, Object> params = null;
        try {
            params = generateParameters(data.getToolParameters(), context.getState().getVariablePool());

            ApiTool apiTool = ToolManager.getApiTool(data.getToolName());
            String response = apiTool.execute(params);
            Map outputs = new HashMap();
            // TODO: support files
            if(Objects.nonNull(data.getResult()) && "json".equals(data.getResult().getType())) {
                outputs.put("result", JsonUtils.fromJson(response, Map.class));
            } else {
                outputs.put("result", response);
            }
            return WorkflowRunState.NodeRunResult.builder()
                    .inputs(params)
                    .outputs(outputs)
                    .status(WorkflowRunState.NodeRunResult.Status.succeeded)
                    .build();
        } catch (Exception e) {
            return WorkflowRunState.NodeRunResult.builder()
                    .inputs(params)
                    .error(e)
                    .status(WorkflowRunState.NodeRunResult.Status.failed)
                    .build();
        }
    }

    private Map<String, Object> generateParameters(Map<String, Data.ToolInput> toolParameters, Map variablePool) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Data.ToolInput> inputEntry : toolParameters.entrySet()) {
            Data.ToolInput toolInput = inputEntry.getValue();
            if(toolInput.getType().equals(Data.ToolInput.Type.mixed.name())) {
                result.put(inputEntry.getKey(), Variables.format(toolInput.getValue().toString(), variablePool));
            } else if(toolInput.getType().equals(Data.ToolInput.Type.variable.name())) {
                result.put(inputEntry.getKey(), Variables.getValue(variablePool, toolInput.getValue().toString()));
            } else if(toolInput.getType().equals(Data.ToolInput.Type.constant.name())) {
                result.put(inputEntry.getKey(), toolInput.getValue());
            }
        }
        return result;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {
        Response result;
        @JsonAlias("provider_id")
        private String providerId;
        @JsonAlias("provider_type")
        private String providerType;
        @JsonAlias("provider_name")
        private String providerName;
        @JsonAlias("tool_name")
        private String toolName;
        @JsonAlias("tool_label")
        private String tooLabel;
        @JsonAlias("tool_configurations")
        private Object toolConfigurations;
        @JsonAlias("tool_parameters")
        private Map<String, ToolInput> toolParameters;

        @Getter
        @Setter
        public static class Response {
            // 'string', 'json'
            String type;
            String data;
        }

        @lombok.Data
        public static class ToolInput {
            private Object value;
            private String type;

            public enum Type {
                mixed,
                variable,
                constant
            }
        }
    }
}
