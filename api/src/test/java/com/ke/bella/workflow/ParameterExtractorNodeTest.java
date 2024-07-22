package com.ke.bella.workflow;

import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.Configs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ParameterExtractorNodeTest {

    @BeforeAll
    public static void initBellaContext() {
        Configs.API_BASE = "https://example.com/v1/";
        BellaContext.setOperator(Operator.builder().userId(userIdL).tenantId("test").userName("test").build());
        BellaContext.setApiKey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8");
    }

    @Test
    public void testToolCallsMode() throws IOException {
        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("inputs", "模型名字是 \"ali-qwen15-72b-chathome-agent-v6-chat-20240314\"");
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/parameter_extractor_tool_calls.json", inputs);

        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
                Assertions.assertEquals("模型名字是 \"ali-qwen15-72b-chathome-agent-v6-chat-20240314\"",
                        context.getState().getWorkflowRunResult().getOutputs().get("inputs"));
                Assertions.assertEquals("ali-qwen15-72b-chathome-agent-v6-chat-20240314",
                        context.getState().getWorkflowRunResult().getOutputs().get("model_service_name"));
                Assertions.assertEquals(1, context.getState().getWorkflowRunResult().getOutputs().get("__is_success"));

            }

            @Override
            public void onWorkflowRunStarted(WorkflowContext context) {
                System.out.println("Workflow run started");
            }

            @Override
            public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
            }

            @Override
            public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
                if("1721358443512".equals(nodeId)) {
                    Map processData = context.getState().getNodeState(nodeId).getProcessData();
                    Assertions.assertNotNull(processData.get("function"));
                    Assertions.assertNotNull(processData.get("tool_call"));
                }
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

    @Test
    public void testPromptMode() throws IOException {
        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("inputs", "模型名字是 \"ali-qwen15-72b-chathome-agent-v6-chat-20240314\"");
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/parameter_extractor_prompts.json", inputs);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
                Assertions.assertEquals("模型名字是 \"ali-qwen15-72b-chathome-agent-v6-chat-20240314\"",
                        context.getState().getWorkflowRunResult().getOutputs().get("inputs"));
                Assertions.assertEquals("ali-qwen15-72b-chathome-agent-v6-chat-20240314",
                        context.getState().getWorkflowRunResult().getOutputs().get("model_service_name"));
                Assertions.assertEquals(1, context.getState().getWorkflowRunResult().getOutputs().get("__is_success"));

            }

            @Override
            public void onWorkflowRunStarted(WorkflowContext context) {
                System.out.println("Workflow run started");
            }

            @Override
            public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
                errorRef.set(t);
            }

            @Override
            public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
                if("1721358443512".equals(nodeId)) {
                    Map processData = context.getState().getNodeState(nodeId).getProcessData();
                    Assertions.assertNotNull(processData.get("llm_text"));
                    Assertions.assertNull(processData.get("tool_call"));
                    Assertions.assertNull(processData.get("function"));
                }
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

        if(errorRef.get() != null) {
            throw new AssertionError("Test failed", errorRef.get());
        }
    }
}
