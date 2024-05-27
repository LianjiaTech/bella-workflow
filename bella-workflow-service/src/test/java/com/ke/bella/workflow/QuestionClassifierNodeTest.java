package com.ke.bella.workflow;

import com.google.common.collect.Maps;
import com.ke.bella.workflow.node.NodeType;
import com.ke.bella.workflow.node.QuestionClassifierNode;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Map;

@Slf4j
public class QuestionClassifierNodeTest {

    @Test
    public void testQuestionClassifierNode() {
        //构造一个 start 节点的输出
        Map output = Maps.newHashMap();
        output.put("query", "如何做家常红烧肉");
        WorkflowRunState.NodeRunResult result = getResult(output);
        Assertions.assertEquals(result.status, WorkflowRunState.NodeRunResult.Status.succeeded);
        Assertions.assertEquals(result.outputs.get("class_name"), "饮食类相关的主题");
        Assertions.assertEquals(result.activatedSourceHandles, Lists.newArrayList("12345678-1234-1234-1234-123456789012"));
    }

    @Test
    public void testQuestionClassifierNode2() {
        //构造一个 start 节点的输出
        Map output = Maps.newHashMap();
        output.put("query", "如何学习打篮球");
        WorkflowRunState.NodeRunResult result = getResult(output);
        Assertions.assertEquals(result.status, WorkflowRunState.NodeRunResult.Status.succeeded);
        Assertions.assertEquals(result.outputs.get("class_name"), "体育类相关的主题");
        Assertions.assertEquals(result.activatedSourceHandles, Lists.newArrayList("23456789-1234-1234-1234-123456789012"));

    }

    private static WorkflowRunState.NodeRunResult getResult(Map output) {
        WorkflowRunState state = new WorkflowRunState();
        state.putNodeState("start", WorkflowRunState.NodeRunResult.builder()
                .outputs(output)
                .status(WorkflowRunState.NodeRunResult.Status.succeeded)
                .build());
        WorkflowContext context = WorkflowContext.builder()
                .tenantId("test")
                .workflowId("test")
                .state(state)
                .build();
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
                                .build()
                ))
                .instruction("Please answer the question in Chinese")
                .timeout(new QuestionClassifierNode.Data.Timeout())
                .authorization(QuestionClassifierNode.Data.Authorization.builder()
                        .apiKey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8")
                        .apiBaseUrl("https://example.com/v1/")
                        .build())
                .build();

        QuestionClassifierNode node = new QuestionClassifierNode(WorkflowSchema.Node.builder()
                .id("question-classifier")
                .type(NodeType.QUESTION_CLASSIFIER.name)
                .height(100)
                .data(JsonUtils.fromJson(JsonUtils.toJson(data), Map.class))
                .build());
        // 执行并校验结果
        WorkflowRunState.NodeRunResult result = node.execute(context, null);
        LOGGER.debug("result: {}", JsonUtils.toJson(result));
        return result;
    }

}
