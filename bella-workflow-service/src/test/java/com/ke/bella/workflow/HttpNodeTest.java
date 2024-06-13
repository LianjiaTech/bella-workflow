package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class HttpNodeTest {

    @Test
    public void testGetMethodWhenNobody() throws IOException {
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/http_node_nobody_case.json", new HashMap<>());
        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
                Assertions.assertThat(context.getState().getWorkflowRunResult().getOutputs().get("body")).isNotNull();
                Assertions.assertThat(context.getState().getWorkflowRunResult().getOutputs().get("status_code")).isNotNull();
                Assertions.assertThat(context.getState().getWorkflowRunResult().getOutputs().get("headers")).isNotNull();
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
