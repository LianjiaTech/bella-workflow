package com.ke.bella.workflow.node;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("rawtypes")
public class IfElseNode extends BaseNode {

    private Data data;

    @SuppressWarnings("unchecked")
    public IfElseNode(Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
        meta.getData().put("source_handles_size", 2);
    }


    @SuppressWarnings("unchecked")
    @Override
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map processData = new LinkedHashMap();
        List<Map<String, Object>> inputConditions = new ArrayList<>();
        for (Data.Condition condition : data.getConditions()) {
            Object actualValue = context.getState().getVariableValue(condition.getVariableSelector());
            Object expectedValue = condition.getValue();

            Map<String, Object> inputCondition = new LinkedHashMap<>();
            inputCondition.put("actual_value", actualValue);
            inputCondition.put("expected_value", expectedValue);
            inputCondition.put("comparison_operator", condition.getComparisonOperator());

            inputConditions.add(inputCondition);
        }
        processData.put("conditions", inputConditions);

        String logicalOperator = data.getLogicalOperator();
        boolean compareResult = logicalOperator.equals("and");
        for (Map<String, Object> inputCondition : inputConditions) {
            Object actualValue = inputCondition.get("actual_value");
            Object expectedValue = inputCondition.get("expected_value");
            String comparisonOperator = (String) inputCondition.get("comparison_operator");
            boolean result = calc(actualValue, expectedValue, comparisonOperator);
            if(compareResult != result) {
                compareResult = result;
                break;
            }
        }

        Map outputs = new LinkedHashMap();
        outputs.put("result", compareResult);
        return NodeRunResult.builder()
                .processData(processData)
                .outputs(outputs)
                .activatedSourceHandles(Arrays.asList(compareResult ? "true" : "false"))
                .status(NodeRunResult.Status.succeeded)
                .build();
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

        List<Condition> conditions;

        @JsonAlias("logical_operator")
        String logicalOperator = "and";
    }
}
