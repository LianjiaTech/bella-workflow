package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class LlmNodeTest {

    @Test
    public void testRunLlmNode() throws IOException {
        Map map = new HashMap();
        map.put("appliance_type", "冰箱");
        map.put("context", "冰箱：美的526；热水器：海尔kl7");
        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/llm_node_case.json", map);
        new WorkflowRunner().run(context, new WorkflowCallbackAdaptor() {
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
