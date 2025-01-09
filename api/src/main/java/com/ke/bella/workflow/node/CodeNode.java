package com.ke.bella.workflow.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.node.BaseNode.BaseNodeData;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.service.code.CodeExecutor;
import com.ke.bella.workflow.service.code.CodeExecutor.CodeLanguage;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class CodeNode extends BaseNode<CodeNode.Data> {

    public CodeNode(WorkflowSchema.Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
    }

    @Override
    protected WorkflowRunState.NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        data.validate();

        Map<String, Object> inputs = new HashMap<>();
        for (WorkflowSchema.Variable variable : data.getVariables()) {
            inputs.put(variable.getVariable(), Variables.getValue(context.getState().getVariablePool(), variable.getValueSelector()));
        }

        return execute0(context, inputs);
    }

    @SuppressWarnings("unchecked")
    private NodeRunResult execute0(WorkflowContext context, Map<String, Object> inputs) {
        Map<String, Object> bindings = new HashMap<>(inputs);
        CodeLanguage language = CodeLanguage.of(data.getCodeLanguage());
        if(language == CodeLanguage.groovy) {
            bindings.put("sys", context.getSys().with(this));
            bindings.put("self", this);
            bindings.put("out", getOut());
        }

        try {
            Object obj = CodeExecutor.execute(language,
                    data.getCode(),
                    bindings,
                    context.getNodeTimeout() - 1, Configs.MAX_EXE_MEMORY_ALLOC);
            if(obj instanceof Map) {
                Map<String, Object> result = transformResult((Map<String, Object>) obj, data.getOutputs(), "", 1);
                return NodeRunResult.builder()
                        .status(NodeRunResult.Status.succeeded)
                        .inputs(inputs)
                        .outputs(result).build();
            } else if(obj instanceof NodeRunResult) {
                ((NodeRunResult) obj).setInputs(inputs);
                return (NodeRunResult) obj;
            } else {
                throw new IllegalArgumentException("代码返回结果类型不是Map");
            }
        } catch (Exception e) {
            return NodeRunResult.builder()
                    .status(NodeRunResult.Status.failed)
                    .inputs(inputs)
                    .error(e).build();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected NodeRunResult resume(WorkflowContext context, IWorkflowCallback callback, Map notifyData) {
        CodeLanguage language = CodeLanguage.of(data.getCodeLanguage());
        if(language == CodeLanguage.groovy) {
            return execute0(context, this.getResumeData().getLastState().getInputs());
        }
        return super.resume(context, callback, notifyData);

    }

    @SuppressWarnings("all")
    public Map<String, Object> transformResult(Map<String, Object> result, Map<String, Data.Output> Output, String prefix, int depth) {

        Map<String, Object> transformedResult = new HashMap<>();
        if(Output == null) {
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                String outputName = entry.getKey();
                Object outputValue = entry.getValue();
                String newPrefix = prefix.isEmpty() ? outputName : prefix + "." + outputName;
                if(outputValue == null) {
                    continue;
                }

                if(outputValue instanceof Map) {
                    transformResult((Map<String, Object>) outputValue, null, newPrefix, depth + 1);
                } else if(outputValue instanceof Number) {
                    checkNumber(outputValue, newPrefix);
                } else if(outputValue instanceof String) {
                    checkString(outputValue, newPrefix);
                } else if(outputValue instanceof Boolean) {
                    checkBoolean(outputValue, newPrefix);
                } else if(outputValue instanceof List) {
                    List<?> outputList = (List<?>) outputValue;
                    if(!outputList.isEmpty()) {
                        Object firstElement = outputList.get(0);
                        if(firstElement instanceof Number && outputList.stream().allMatch(Number.class::isInstance)) {
                            for (int i = 0; i < outputList.size(); i++) {
                                checkNumber(outputList.get(i), newPrefix + "[" + i + "]");
                            }
                        } else if(firstElement instanceof String && outputList.stream().allMatch(String.class::isInstance)) {
                            for (int i = 0; i < outputList.size(); i++) {
                                checkString(outputList.get(i), newPrefix + "[" + i + "]");
                            }
                        } else if(firstElement instanceof Map && outputList.stream().allMatch(Map.class::isInstance)) {
                            for (int i = 0; i < outputList.size(); i++) {
                                transformResult((Map<String, Object>) outputList.get(i), null, newPrefix + "[" + i + "]", depth + 1);
                            }
                        } else {
                            throw new IllegalArgumentException(
                                    "output " + newPrefix + " is not a valid array. Make sure all elements are of the same type.");
                        }
                    }
                } else {
                    throw new IllegalArgumentException("output " + newPrefix + " is not a valid type.");
                }
            }
            return result;
        }

        Map<String, Boolean> parametersValidated = new HashMap<>();
        for (Map.Entry<String, Data.Output> entry : Output.entrySet()) {
            String outputName = entry.getKey();
            Data.Output outputConfig = entry.getValue();
            String dot = prefix.isEmpty() ? "" : ".";
            if(!result.containsKey(outputName)) {
                throw new IllegalArgumentException("output " + prefix + dot + outputName + " is missing.");
            }

            switch (outputConfig.getType()) {
            case "object":
                if(!(result.get(outputName) instanceof Map)) {
                    throw new IllegalArgumentException(
                            "output " + prefix + dot + outputName + " is not an object, got " + result.get(outputName).getClass() + " instead.");
                }
                transformedResult.put(outputName, transformResult((Map<String, Object>) result.get(outputName), outputConfig.getChildren(),
                        prefix + dot + outputName, depth + 1));
                break;
            case "number":
                transformedResult.put(outputName, checkNumber(result.get(outputName), prefix + dot + outputName));
                break;
            case "string":
                transformedResult.put(outputName, checkString(result.get(outputName), prefix + dot + outputName));
                break;
            case "array[number]":
                if(!(result.get(outputName) instanceof List)) {
                    throw new IllegalArgumentException(
                            "output " + prefix + dot + outputName + " is not an array, got " + result.get(outputName).getClass() + " instead.");
                }
                List<?> numberList = (List<?>) result.get(outputName);
                List<Object> checkedNumberList = new ArrayList<>();
                for (int i = 0; i < numberList.size(); i++) {
                    checkedNumberList.add(checkNumber(numberList.get(i), prefix + dot + outputName + "[" + i + "]"));
                }
                transformedResult.put(outputName, checkedNumberList);
                break;
            case "array[string]":
                if(!(result.get(outputName) instanceof List)) {
                    throw new IllegalArgumentException(
                            "output " + prefix + dot + outputName + " is not an array, got " + result.get(outputName).getClass() + " instead.");
                }
                List<?> stringList = (List<?>) result.get(outputName);
                List<Object> checkedStringList = new ArrayList<>();
                for (int i = 0; i < stringList.size(); i++) {
                    checkedStringList.add(checkString(stringList.get(i), prefix + dot + outputName + "[" + i + "]"));
                }
                transformedResult.put(outputName, checkedStringList);
                break;
            case "array[object]":
                if(!(result.get(outputName) instanceof List)) {
                    throw new IllegalArgumentException(
                            "output " + prefix + dot + outputName + " is not an array, got " + result.get(outputName).getClass() + " instead.");
                }
                List<?> objectList = (List<?>) result.get(outputName);
                for (int i = 0; i < objectList.size(); i++) {
                    if(!(objectList.get(i) instanceof Map)) {
                        throw new IllegalArgumentException("output " + prefix + dot + outputName + "[" + i + "] is not an object, got "
                                + objectList.get(i).getClass() + " instead at index " + i + ".");
                    }
                }
                List<Object> checkedObjectList = new ArrayList<>();
                for (int i = 0; i < objectList.size(); i++) {
                    checkedObjectList.add(transformResult((Map<String, Object>) objectList.get(i), outputConfig.getChildren(),
                            prefix + dot + outputName + "[" + i + "]", depth + 1));
                }
                transformedResult.put(outputName, checkedObjectList);
                break;
            default:
                throw new IllegalArgumentException("output type " + outputConfig.getType() + " is not supported.");
            }

            parametersValidated.put(outputName, true);
        }

        if(parametersValidated.size() != result.size()) {
            throw new IllegalArgumentException("Not all output parameters are validated.");
        }

        return transformedResult;
    }

    private Object checkNumber(Object value, String variable) {
        if(!(value instanceof Number)) {
            throw new IllegalArgumentException("Variable " + variable + " is not a number.");
        }
        return value;
    }

    private Object checkString(Object value, String variable) {
        if(value != null && !(value instanceof String)) {
            throw new IllegalArgumentException("Variable " + variable + " is not a string.");
        }
        return value;
    }

    private Object checkBoolean(Object value, String variable) {
        if(!(value instanceof Boolean)) {
            throw new IllegalArgumentException("Variable " + variable + " is not a bool.");
        }
        return value;
    }

    public static Map<String, Object> defaultConfig(Map<String, Object> filters) {
        CodeLanguage codeLanguage = CodeLanguage.python3;
        if(!CollectionUtils.isEmpty(filters) && filters.containsKey("code_language")) {
            codeLanguage = CodeLanguage.of((String) filters.get("code_language"));
        }
        return CodeExecutor.getDefaultConfig(codeLanguage);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Data extends BaseNodeData {
        private List<WorkflowSchema.Variable> variables;
        @JsonProperty("code_language")
        private String codeLanguage;
        private String code;
        private Map<String, Output> outputs;

        public void validate() {
            Stream.of(CodeLanguage.values())
                    .filter(i -> i.name().equals(codeLanguage))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("invalid code language %s", codeLanguage)));
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Output {
            private String type;
            private Map<String, Output> children;
        }
    }
}
