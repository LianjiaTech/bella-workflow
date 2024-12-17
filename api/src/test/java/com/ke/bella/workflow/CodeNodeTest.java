package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.ke.bella.workflow.service.Configs;

public class CodeNodeTest extends CommonNodeTest {

    @Test
    public void testPython3() throws IOException {
        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("input1", "1");
        inputs.put("input2", "2");
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/code_node_python3.json", inputs);

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

        if(errorRef.get() != null) {
            throw new AssertionError("Test failed", errorRef.get());
        }
    }

    @Test
    public void testJavaScript() throws IOException {
        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("input1", "1");
        inputs.put("input2", "2");
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/code_node_javascript.json", inputs);
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

        if(errorRef.get() != null) {
            throw new AssertionError("Test failed", errorRef.get());
        }
    }
}
