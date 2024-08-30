package com.ke.bella.workflow.node;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.node.BaseNode.BaseNodeData;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("rawtypes")
public class IfElseNode extends BaseNode<IfElseNode.Data> {

    @SuppressWarnings("unchecked")
    public IfElseNode(Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
        meta.getData().put("source_handles_size", this.data.getCases().size() + 1);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map inputs = new HashMap();
        Map processData = new LinkedHashMap();
        List<Map> conditionResults = new ArrayList<>();
        String selectedCaseId = null;
        boolean finalResult = false;
        List<Map<String, Object>> inputConditions = null;
        try {
            for (Data.Case _case : data.getCases()) {
                CompareResult compareResult = compareCondition(_case.getLogicalOperator(), _case.getConditions(), context);

                finalResult = _case.getLogicalOperator().equals("and") ? compareResult.getGroupResult().stream().allMatch(Boolean::booleanValue)
                        : compareResult.getGroupResult().stream().anyMatch(Boolean::booleanValue);

                inputConditions = compareResult.getInputConditions();

                Map<String, Object> conditionResult = new HashMap<>();
                conditionResult.put("group", _case);
                conditionResult.put("results", compareResult.getGroupResult());
                conditionResult.put("final_result", finalResult);

                conditionResults.add(conditionResult);

                if(finalResult) {
                    selectedCaseId = _case.getCaseId();
                    break;
                }
            }
            inputs.put("conditions", inputConditions);
            processData.put("condition_results", conditionResults);
            Map outputs = new LinkedHashMap();
            outputs.put("result", finalResult);
            outputs.put("selected_case_id", selectedCaseId);
            String activeHandle = Objects.nonNull(selectedCaseId) ? selectedCaseId : "false";

            return NodeRunResult.builder()
                    .processData(processData)
                    .inputs(inputs)
                    .outputs(outputs)
                    .activatedSourceHandles(Collections.singletonList(activeHandle))
                    .status(NodeRunResult.Status.succeeded)
                    .build();
        } catch (Exception e) {
            return NodeRunResult.builder()
                    .processData(processData)
                    .inputs(inputs)
                    .error(e)
                    .status(NodeRunResult.Status.failed)
                    .build();
        }

    }

    private CompareResult compareCondition(String logicalOperator, List<Data.Condition> conditions, WorkflowContext context) {
        List<Map<String, Object>> inputConditions = new ArrayList<>();

        for (Data.Condition condition : conditions) {
            Object actualValue = context.getState().getVariableValue(condition.getVariableSelector());
            Object expectedValue = condition.getValue();

            Map<String, Object> inputCondition = new LinkedHashMap<>();
            inputCondition.put("actual_value", actualValue);
            inputCondition.put("expected_value", expectedValue);
            inputCondition.put("comparison_operator", condition.getComparisonOperator());

            inputConditions.add(inputCondition);
        }
        List<Boolean> groupResult = new ArrayList<>(Collections.nCopies(inputConditions.size(), false));
        boolean compareResult = logicalOperator.equals("and");
        for (int i = 0; i < inputConditions.size(); i++) {
            Map<String, Object> inputCondition = inputConditions.get(i);
            Object actualValue = inputCondition.get("actual_value");
            Object expectedValue = inputCondition.get("expected_value");
            String comparisonOperator = (String) inputCondition.get("comparison_operator");
            boolean result = calc(actualValue, expectedValue, comparisonOperator);
            groupResult.set(i, result);
            if(compareResult != result) {
                break;
            }
        }
        return CompareResult.builder().inputConditions(inputConditions).groupResult(groupResult).build();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @lombok.Data
    @Builder
    private static class CompareResult {
        private List<Map<String, Object>> inputConditions;
        private List<Boolean> groupResult;
    }

    private boolean calc(Object actualValue, Object expectedValue, String op) {
        if(op.equals("contains")) {
            return assertContains(actualValue, (String) expectedValue);
        } else if(op.equals("not contains")) {
            return assertNotContains(actualValue, (String) expectedValue);
        } else if(op.equals("start with")) {
            return assertStartWith(actualValue, (String) expectedValue);
        } else if(op.equals("end with")) {
            return assertEndWith(actualValue, (String) expectedValue);
        } else if(op.equals("is")) {
            return assertIs(actualValue, (String) expectedValue);
        } else if(op.equals("is not")) {
            return assertIsNot(actualValue, (String) expectedValue);
        } else if(op.equals("empty")) {
            return assertEmpty(actualValue);
        } else if(op.equals("not empty")) {
            return assertNotEmpty(actualValue);
        } else if(op.equals("=")) {
            return assertEq(actualValue, (String) expectedValue);
        } else if(op.equals("≠")) {
            return assertNotEq(actualValue, (String) expectedValue);
        } else if(op.equals(">")) {
            return assertGreaterThan(actualValue, (String) expectedValue);
        } else if(op.equals("<")) {
            return assertLessThan(actualValue, (String) expectedValue);
        } else if(op.equals("≥")) {
            return assertGreaterThanOrEqual(actualValue, (String) expectedValue);
        } else if(op.equals("≤")) {
            return assertLessThanOrEqual(actualValue, (String) expectedValue);
        } else if(op.equals("null")) {
            return actualValue == null;
        } else if(op.equals("not null")) {
            return actualValue != null;
        }
        return false;
    }

    private boolean assertContains(Object actualValue, String expectedValue) {
        if(actualValue == null) {
            return false;
        }

        if(actualValue instanceof String) {
            return ((String) actualValue).contains(expectedValue);
        } else if(actualValue instanceof List) {
            return ((List<?>) actualValue).contains(expectedValue);
        } else {
            throw new IllegalArgumentException("Invalid actual value type: string or list");
        }
    }

    private boolean assertNotContains(Object actualValue, String expectedValue) {
        return !assertContains(actualValue, expectedValue);
    }

    private boolean assertStartWith(Object actualValue, String expectedValue) {
        if(actualValue == null) {
            return false;
        }

        if(actualValue instanceof String) {
            return ((String) actualValue).startsWith(expectedValue);
        } else {
            throw new IllegalArgumentException("Invalid actual value type: string or list");
        }
    }

    private boolean assertEndWith(Object actualValue, String expectedValue) {
        if(actualValue == null) {
            return false;
        }

        if(actualValue instanceof String) {
            return ((String) actualValue).endsWith(expectedValue);
        } else {
            throw new IllegalArgumentException("Invalid actual value type: string or list");
        }
    }

    private boolean assertIs(Object actualValue, String expectedValue) {
        if(actualValue == null) {
            return false;
        }

        if(actualValue instanceof String) {
            return ((String) actualValue).equals(expectedValue);
        } else {
            throw new IllegalArgumentException("Invalid actual value type: string or list");
        }
    }

    private boolean assertIsNot(Object actualValue, String expectedValue) {
        return !assertIs(actualValue, expectedValue);
    }

    private boolean assertEmpty(Object actualValue) {
        if(actualValue == null) {
            return true;
        }
        if(actualValue instanceof String) {
            return ((String) actualValue).length() == 0;
        } else if(actualValue instanceof List) {
            return ((List) actualValue).isEmpty();
        }
        return false;
    }

    private boolean assertNotEmpty(Object actualValue) {
        return !assertEmpty(actualValue);
    }

    private boolean assertEq(Object actualValue, String expectedValue) {
        if(actualValue == null) {
            return false;
        }

        if(!(actualValue instanceof Number)) {
            return false;
        }

        return new BigDecimal(expectedValue).equals(new BigDecimal(actualValue.toString()));
    }

    private boolean assertNotEq(Object actualValue, String expectedValue) {
        return !assertEq(actualValue, expectedValue);
    }

    private boolean assertGreaterThan(Object actualValue, String expectedValue) {
        if(actualValue == null) {
            return false;
        }

        if(!(actualValue instanceof Number)) {
            return false;
        }

        return new BigDecimal(actualValue.toString()).compareTo(new BigDecimal(expectedValue)) > 0;
    }

    private boolean assertLessThan(Object actualValue, String expectedValue) {
        return !assertGreaterThan(actualValue, expectedValue);
    }

    private boolean assertGreaterThanOrEqual(Object actualValue, String expectedValue) {
        if(actualValue == null) {
            return false;
        }

        if(!(actualValue instanceof Number)) {
            return false;
        }

        return new BigDecimal(actualValue.toString()).compareTo(new BigDecimal(expectedValue)) >= 0;
    }

    private boolean assertLessThanOrEqual(Object actualValue, String expectedValue) {
        if(actualValue == null) {
            return false;
        }

        if(!(actualValue instanceof Number)) {
            return false;
        }

        return new BigDecimal(actualValue.toString()).compareTo(new BigDecimal(expectedValue)) <= 0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Condition {
            @JsonAlias("variable_selector")
            List<String> variableSelector;

            @JsonAlias("comparison_operator")
            String comparisonOperator;

            String value;
        }

        @lombok.Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Case {
            String id;
            @JsonProperty("case_id")
            String caseId;
            @JsonProperty("logical_operator")
            String logicalOperator;
            List<Condition> conditions;
        }

        List<Condition> conditions;

        @JsonAlias("logical_operator")
        String logicalOperator = "and";
        List<Case> cases;

        // fixme: 中间逻辑：if-else 老协议转新协议
        public List<Case> getCases() {
            if(CollectionUtils.isEmpty(cases)) {
                Case aCase = new Case();
                aCase.setLogicalOperator(logicalOperator);
                aCase.setConditions(conditions);
                aCase.setId("true");
                aCase.setCaseId("true");
                return Collections.singletonList(aCase);
            } else {
                return cases;
            }
        }

        @Override
        public List<String> getSourceHandles() {
            List<String> ret = getCases().stream().map(Case::getCaseId).collect(Collectors.toList());
            ret.add("false");
            return ret;
        }
    }
}
