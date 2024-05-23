package com.ke.bella.workflow;

import java.util.HashMap;

import org.junit.Test;

public class WorkflowRunnerTest {
    @Test
    public void testWorkflowOnlyStartNode() {
        WorkflowSchema meta = JsonUtils.fromJson(
                "{\"graph\":{\"edges\":[{\"data\":{\"sourceType\":\"start\",\"targetType\":\"end\"},\"id\":\"1715592079631-1716360197363\",\"source\":\"1715592079631\",\"sourceHandle\":\"source\",\"target\":\"1716360197363\",\"targetHandle\":\"target\",\"type\":\"custom\"}],\"nodes\":[{\"data\":{\"desc\":\"\",\"selected\":false,\"title\":\"开始\",\"type\":\"start\",\"variables\":[{\"label\":\"aa\",\"max_length\":48,\"options\":[],\"required\":true,\"type\":\"text-input\",\"variable\":\"a\"}]},\"height\":90,\"id\":\"1715592079631\",\"position\":{\"x\":78,\"y\":282},\"positionAbsolute\":{\"x\":78,\"y\":282},\"selected\":false,\"sourcePosition\":\"right\",\"targetPosition\":\"left\",\"type\":\"custom\",\"width\":244},{\"data\":{\"desc\":\"\",\"outputs\":[{\"value_selector\":[\"1715592079631\",\"a\"],\"variable\":\"a\"}],\"selected\":false,\"title\":\"结束\",\"type\":\"end\"},\"height\":90,\"id\":\"1716360197363\",\"position\":{\"x\":378,\"y\":282},\"positionAbsolute\":{\"x\":378,\"y\":282},\"selected\":true,\"sourcePosition\":\"right\",\"targetPosition\":\"left\",\"type\":\"custom\",\"width\":244}],\"viewport\":{\"x\":-88,\"y\":39,\"zoom\":1}}}",
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
