package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContextValidateTest {

    @Test
    public void whenLostRequiredInputsThrowsIllegalArgumentException() throws IOException {
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/lost_required_inputs.json", new HashMap<>());
        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
            @Override
            public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
                Assertions.assertTrue(t instanceof IllegalArgumentException);
            }
        });
    }
}
