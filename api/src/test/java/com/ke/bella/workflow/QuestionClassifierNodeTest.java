package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import com.google.common.collect.Maps;
import com.ke.bella.workflow.node.NodeType;
import com.ke.bella.workflow.node.QuestionClassifierNode;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuestionClassifierNodeTest {

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testQuestionClassifierNode() throws IOException {
        // 构造一个 start 节点的输出
        Map output = Maps.newHashMap();
        output.put("query", "如何做家常红烧肉");
        WorkflowRunState.NodeRunResult result = getResult(output);
        Assertions.assertEquals(result.status, WorkflowRunState.NodeRunResult.Status.succeeded);
        Assertions.assertEquals(result.outputs.get("class_name"), "饮食类相关的主题");
        Assertions.assertEquals(result.activatedSourceHandles, Lists.newArrayList("12345678-1234-1234-1234-123456789012"));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testQuestionClassifierNode2() throws IOException {
        // 构造一个 start 节点的输出
        Map output = Maps.newHashMap();
        output.put("query", "如何学习打篮球");
        WorkflowRunState.NodeRunResult result = getResult(output);
        Assertions.assertEquals(result.status, WorkflowRunState.NodeRunResult.Status.succeeded);
        Assertions.assertEquals(result.outputs.get("class_name"), "体育类相关的主题");
        Assertions.assertEquals(result.activatedSourceHandles, Lists.newArrayList("23456789-1234-1234-1234-123456789012"));

    }

    @SuppressWarnings({ "rawtypes" })
    private static WorkflowRunState.NodeRunResult getResult(Map output) throws IOException {
        WorkflowRunState state = new WorkflowRunState();
        state.putNodeState("start", WorkflowRunState.NodeRunResult.builder()
                .outputs(output)
                .status(WorkflowRunState.NodeRunResult.Status.succeeded)
                .build());
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/question_classifier_node_case.json", new HashMap());
        context.setState(state);
        // 构造一个 QuestionClassifierNode
        QuestionClassifierNode.Data data = QuestionClassifierNode.Data.builder()
                .queryVariableSelector(Lists.newArrayList("start", "query"))
                .model(QuestionClassifierNode.Data.ModelConfig.builder()
                        .name("c4ai-command-r-plus")
                        .param(QuestionClassifierNode.Data.CompletionParam.builder()
                                .presencePenalty(1d)
                                .frequencyPenalty(1d)
                                .maxTokens(512)
                                .topP(0.7)
                                .temperature(0.7)
                                .build())
                        .build())
                .classes(Lists.newArrayList(
                        QuestionClassifierNode.Data.ClassConfig.builder()
                                .id("12345678-1234-1234-1234-123456789012")
                                .name("饮食类相关的主题")
                                .build(),
                        QuestionClassifierNode.Data.ClassConfig.builder()
                                .id("23456789-1234-1234-1234-123456789012")
                                .name("体育类相关的主题")
                                .build()))
                .instruction("Please answer the question in Chinese")
                .timeout(new QuestionClassifierNode.Data.Timeout())
                .authorization(QuestionClassifierNode.Data.Authorization.builder()
                        .apiKey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8")
                        .apiBaseUrl("https://example.com/v1/")
                        .build())
                .build();

        QuestionClassifierNode node = new QuestionClassifierNode(WorkflowSchema.Node.builder()
                .id("1716904682038")
                .type(NodeType.QUESTION_CLASSIFIER.name)
                .height(100)
                .data(JsonUtils.fromJson(JsonUtils.toJson(data), Map.class))
                .build());
        // 执行并校验结果
        WorkflowRunState.NodeRunResult result = node.run(context, new WorkflowCallbackAdaptor());
        LOGGER.debug("result: {}", JsonUtils.toJson(result));
        return result;
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testQuestionClassifierNodeResult() throws IOException {
        HashMap<String, Object> userInputs = new HashMap<>();
        userInputs.put("input", "情感咨询");
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/question_classifier_node_case.json", userInputs);
        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
                Map outputs = context.getState().getWorkflowRunResult().getOutputs();
                Assertions.assertNotNull(outputs);
                Assertions.assertTrue(outputs.containsKey("class_name"));
                Assertions.assertTrue(outputs.containsKey("output"));
                Assertions.assertEquals("情感咨询", outputs.get("output"));
                Assertions.assertEquals("情感咨询", outputs.get("class_name"));

                Assertions.assertNotNull(outputs);
            }

            @Override
            public void onWorkflowRunStarted(WorkflowContext context) {
            }

            @Override
            public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
            }

            @Override
            public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
            }

            @Override
            public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
                System.out.println("Node run started: " + nodeId);

            }

            @Override
            public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData data) {
                System.out.println("Node run progress: " + nodeId + " processData: " + JsonUtils.toJson(data));

            }

            @Override
            public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
            }

            @Override
            public void onWorkflowRunSuspended(WorkflowContext context) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onWorkflowRunResumed(WorkflowContext context) {
                // TODO Auto-generated method stub

            }
        });

    }

}
