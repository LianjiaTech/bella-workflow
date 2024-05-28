package com.ke.bella.workflow;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class LlmNodeTest {

    @Test
    public void testRunLlmNode() {
        WorkflowSchema meta = JsonUtils.fromJson(
                "{\"graph\":{\"edges\":[{\"data\":{\"sourceType\":\"start\",\"targetType\":\"llm\"},\"id\":\"1716645578090-1716865089006\",\"source\":\"1716645578090\",\"sourceHandle\":\"source\",\"target\":\"1716865089006\",\"targetHandle\":\"target\",\"type\":\"custom\"},{\"data\":{\"sourceType\":\"llm\",\"targetType\":\"end\"},\"id\":\"1716865089006-1716865217712\",\"source\":\"1716865089006\",\"sourceHandle\":\"source\",\"target\":\"1716865217712\",\"targetHandle\":\"target\",\"type\":\"custom\"}],\"nodes\":[{\"data\":{\"desc\":\"\",\"selected\":false,\"title\":\"开始\",\"type\":\"start\",\"variables\":[{\"label\":\"input\",\"max_length\":48,\"options\":[],\"required\":true,\"type\":\"text-input\",\"variable\":\"input\"},{\"label\":\"context\",\"max_length\":48,\"options\":[],\"required\":true,\"type\":\"text-input\",\"variable\":\"context\"}]},\"height\":116,\"id\":\"1716645578090\",\"position\":{\"x\":571.6099888564995,\"y\":282},\"positionAbsolute\":{\"x\":571.6099888564995,\"y\":282},\"selected\":false,\"sourcePosition\":\"right\",\"targetPosition\":\"left\",\"type\":\"custom\",\"width\":244},{\"data\":{\"authorization\":{\"apiKey\":\"LpTgbfOUJeDRT8LzSTS2Iqh8zkHQPyWs\"},\"context\":{\"enabled\":true,\"variable_selector\":[\"1716645578090\",\"context\"]},\"desc\":\"\",\"model\":{\"completion_params\":{\"temperature\":0.7},\"mode\":\"chat\",\"name\":\"gpt-4\",\"provider\":\"openai\"},\"prompt_template\":[{\"id\":\"4c888b63-abb8-42ef-bfc9-fdebca1e740b\",\"role\":\"system\",\"text\":\"你是一位资深的家电导购，你需要按照销售清单对顾客进行家电推荐，下边是你本次的销售清单：{{#context#}}\"},{\"id\":\"f9ee657c-e2be-4b08-a508-58564dfea96b\",\"role\":\"user\",\"text\":\"{{#1716645578090.input#}}\"}],\"selected\":false,\"title\":\"LLM\",\"type\":\"llm\",\"variables\":[],\"vision\":{\"enabled\":false}},\"height\":98,\"id\":\"1716865089006\",\"position\":{\"x\":900.4885611281284,\"y\":286.55977456920454},\"positionAbsolute\":{\"x\":900.4885611281284,\"y\":286.55977456920454},\"selected\":false,\"sourcePosition\":\"right\",\"targetPosition\":\"left\",\"type\":\"custom\",\"width\":244},{\"data\":{\"desc\":\"\",\"outputs\":[{\"value_selector\":[\"1716865089006\",\"text\"],\"variable\":\"text\"}],\"selected\":false,\"title\":\"结束\",\"type\":\"end\"},\"height\":90,\"id\":\"1716865217712\",\"position\":{\"x\":1197.4487114153253,\"y\":286.55977456920454},\"positionAbsolute\":{\"x\":1197.4487114153253,\"y\":286.55977456920454},\"selected\":true,\"sourcePosition\":\"right\",\"targetPosition\":\"left\",\"type\":\"custom\",\"width\":244}],\"viewport\":{\"x\":-224.72309997542504,\"y\":133.7260761076987,\"zoom\":0.6579272625145054}}}",
                WorkflowSchema.class);
        WorkflowGraph graph = new WorkflowGraph(meta);
        Map map = new HashMap();
        map.put("input", "我想买冰箱");
        map.put("context", "冰箱：美的526；热水器：海尔kl7");
        WorkflowContext context = WorkflowContext.builder()
                .graph(graph)
                .state(new WorkflowRunState())
                .userInputs(map)
                .build();
        new WorkflowRunner().run(context, new IWorkflowCallback() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
                System.out.println("Workflow run succeeded: " + JsonUtils.toJson(context.getWorkflowRunResult()));
            }

            @Override
            public void onWorkflowRunStarted(WorkflowContext context) {
                System.out.println("Workflow run started");
            }

            @Override
            public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
            }

            @Override
            public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
                Assertions.assertThat(context.getState().getNodeState(nodeId).getStatus()).isEqualTo(WorkflowRunState.NodeRunResult.Status.succeeded);
            }

            @Override
            public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId) {
                System.out.println("Node run started: " + nodeId);

            }

            @Override
            public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, ProgressData data) {
                System.out.println("Node run progress: " + nodeId + " processData: " + JsonUtils.toJson(data));

            }

            @Override
            public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
                System.out.println("Node run failed: " + nodeId + " " + error);
                t.printStackTrace();
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
