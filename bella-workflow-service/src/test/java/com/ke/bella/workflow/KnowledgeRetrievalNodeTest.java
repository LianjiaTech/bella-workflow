package com.ke.bella.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.node.KnowledgeRetrievalNode;
import com.ke.bella.workflow.service.Configs;

public class KnowledgeRetrievalNodeTest {

    @BeforeAll
    public static void initBellaContext() {
        Configs.API_BASE = "https://example.com/v1/";
        BellaContext.setOperator(Operator.builder().userId(userIdL).tenantId("test").userName("test").build());
        BellaContext.setApiKey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8");
    }

    @Test
    public void testKnowledgeRetrievalNode() throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        map.put("query", "Windows/MAC-深信服-VPN如何安装使用");

        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/knowledge_retrieval_node_test.json", map);
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
                if("1718790018841".equals(nodeId)) {
                    WorkflowRunState.NodeRunResult result = context.getState().getNodeState(nodeId);
                    Assertions.assertNotNull(result);
                    Assertions.assertNotNull(result.getOutputs());
                    List<KnowledgeRetrievalNode.KnowledgeRetrievalResult> resultList = (List) result.getOutputs().get("result");
                    Assertions.assertFalse(CollectionUtils.isEmpty(resultList));
                    for (KnowledgeRetrievalNode.KnowledgeRetrievalResult entity : resultList) {
                        Assertions.assertNotNull(entity.getTitle());
                        Assertions.assertNotNull(entity.getContent());
                        Assertions.assertNotNull(entity.getMetadata());
                    }
                }
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

    @Test
    public void testKnowledgeRetrievalNode2() throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        map.put("query", "Windows/MAC-深信服-VPN如何安装使用");

        WorkflowContext context = CommonNodeTest.createContext("src/test/resources/knowledge_retrieval_node_test.json", map);
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
                if("1718790018841".equals(nodeId)) {
                    WorkflowRunState.NodeRunResult result = context.getState().getNodeState(nodeId);
                    Assertions.assertNotNull(result);
                    Assertions.assertNotNull(result.getOutputs());
                    List<KnowledgeRetrievalNode.KnowledgeRetrievalResult> resultList = (List) result.getOutputs().get("result");
                    Assertions.assertFalse(CollectionUtils.isEmpty(resultList));
                    for (KnowledgeRetrievalNode.KnowledgeRetrievalResult entity : resultList) {
                        Assertions.assertNotNull(entity.getTitle());
                        Assertions.assertNotNull(entity.getContent());
                        Assertions.assertNotNull(entity.getMetadata());
                    }
                }
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
