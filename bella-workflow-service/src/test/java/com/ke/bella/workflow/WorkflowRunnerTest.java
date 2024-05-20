package com.ke.bella.workflow;

import java.util.HashMap;

import org.junit.Test;

import com.ke.bella.workflow.node.Utils;

public class WorkflowRunnerTest {
    @Test
    public void testWorkflowOnlyStartNode() {
        WorkflowSchema meta = Utils.fromJson(
                "{\"graph\":{\"edges\":[],\"nodes\":[{\"data\":{\"desc\":\"\",\"selected\":false,\"title\":\"Start\",\"type\":\"start\",\"variables\":[{\"label\":\"ss是\",\"max_length\":48,\"options\":[],\"required\":true,\"type\":\"text-input\",\"variable\":\"ss\"}]},\"dragging\":false,\"height\":90,\"id\":\"1711527768326\",\"position\":{\"x\":0.24229431219265507,\"y\":250.87504168280685},\"positionAbsolute\":{\"x\":0.24229431219265507,\"y\":250.87504168280685},\"selected\":false,\"sourcePosition\":\"right\",\"targetPosition\":\"left\",\"type\":\"custom\",\"width\":244}],\"viewport\":{\"x\":4.301986877937907,\"y\":-65.75965914432777,\"zoom\":1.028113826656067}}}",
                WorkflowSchema.class);
        WorkflowGraph graph = new WorkflowGraph(meta);
        WorkflowContext context = WorkflowContext.builder()
                .graph(graph)
                .state(new WorkflowRunState())
                .userInputs(new HashMap<>())
                .build();
        new WorkflowRunner().run(context, new IWorkflowCallback() {
            @Override
            public void onWorkflowRunSucceeded(WorkflowContext context) {
				System.out.println("Workflow run succeeded");
            }

            @Override
            public void onWorkflowRunStarted(WorkflowContext context) {
				System.out.println("Workflow run started");
            }

            @Override
            public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
                System.out.println("Workflow run failed: " + error);
                t.printStackTrace();
            }

            @Override
            public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
                System.out.println("Node run succeeded: " + nodeId);

            }

            @Override
            public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId) {
                System.out.println("Node run started: " + nodeId);

            }

            @Override
            public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId) {
                System.out.println("Node run progress: " + nodeId);

            }

            @Override
            public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
                System.out.println("Node run failed: " + nodeId + " " + error);
                t.printStackTrace();
            }
        });
    }
}
