package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ke.bella.workflow.utils.JsonUtils;
import com.theokanning.openai.completion.chat.ChatCompletionResult;

public class ToolNodeTest extends CommonNodeTest {

    @Test
    public void testGet() throws IOException, InterruptedException {
        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("model_service_name", "ali-qwen25-72b-base-v1-chat-20250122");
        inputs.put("lianjia_cookie", "lianjia_ssid=1787e6f7-06e9-4b59-9519-edc11524e633; lianjia_uuid=12eccb7b-0906-4113-a6d1-7a458c48801e");
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/tool_node_get.json", inputs);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
                try {
                    Assertions.assertEquals("{\"status\":\"ok\"}", context.getState().getWorkflowRunResult().getOutputs().get("result"));
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onWorkflowRunStarted(WorkflowContext context) {
                System.out.println("Workflow run started");
            }

            @Override
            public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
                errorRef.set(t);
                latch.countDown();
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

        latch.await();
        if(errorRef.get() != null) {
            throw new AssertionError("Test failed", errorRef.get());
        }
    }

    @Test
    public void testPostWithCredentials() throws IOException, InterruptedException {
        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("top_p", 0.1);
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/tool_node_post_with_credentials.json", inputs);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
                try {
                    String o = (String) context.getState().getWorkflowRunResult().getOutputs().get("result");
                    ChatCompletionResult chatCompletionResult = JsonUtils.fromJson(o, ChatCompletionResult.class);
                    Assertions.assertNotNull(chatCompletionResult);
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onWorkflowRunStarted(WorkflowContext context) {
                System.out.println("Workflow run started");
            }

            @Override
            public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
                errorRef.set(t);
                latch.countDown();
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

        latch.await();
        if(errorRef.get() != null) {
            throw new AssertionError("Test failed", errorRef.get());
        }
    }
}
