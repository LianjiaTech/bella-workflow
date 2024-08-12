package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpNodeTest {

    static OkHttpClient client = new OkHttpClient.Builder()
            .build();

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
            public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
                Assertions.assertThat(context.getState().getNodeState(nodeId).getStatus()).isEqualTo(WorkflowRunState.NodeRunResult.Status.succeeded);
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

    @Test
    public void testResponseOverLimit() throws IOException {
        Request request = new Request.Builder()
                .url("http://example.com/bclever/bella/ali-qwen15-72b-chathome-v2-chat-20240807/healthCheck")
                .method("GET", null)
                .build();
        Response resp = client.newCall(request).execute();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> HttpUtils.readBodyWithinLimit(resp.body(), 1));
        Response resp2 = client.newCall(request).execute();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> HttpUtils.readBodyWithinLimit(resp2.body(), 200));

    }
}
