package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.ke.bella.workflow.service.Configs;

public class TemplateTransformNodeTest extends CommonNodeTest {

    @BeforeAll
    public static void initOpenAPIContext() {
        Configs.OPEN_API_BASE = "https://example.com/v1/";
    }

    @Test
    public void testJinja2() throws IOException {
        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("input1", "1");
        inputs.put("input2", "2");
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/template_transform_node.json", inputs);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
                Assertions.assertEquals("12", context.getWorkflowRunResult().getOutputs().get("result").toString());
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
