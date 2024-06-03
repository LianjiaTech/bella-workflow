package com.ke.bella.workflow.dify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.andrewoma.dexx.collection.Maps;
import com.ke.bella.workflow.AbstractTest;
import com.ke.bella.workflow.BellaContext;
import com.ke.bella.workflow.IDGenerator;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.api.DifyController;
import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.api.callbacks.DifyWorkflowRunStreamingCallback;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Transactional
public class DifyControllerTest extends AbstractTest {

    @Autowired
    private DifyController dify;

    static {
        BellaContext.setOperator(Operator.builder().userId(userIdL).tenantId("test").userName("test").build());
        BellaContext.setApiKey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8");
    }

    @Test
    public void testCreateDraft() {
        String WORKFLOW_ID = IDGenerator.newWorkflowId();
        Example<WorkflowSchema, Map> source = readJson("post-apps-{workflowId}-workflow-draft.json",
                new TypeReference<Example<WorkflowSchema, Map>>() {
                });
        dify.saveDraftInfo(WORKFLOW_ID, source.getRequest());
        WorkflowSchema target = dify.getDraftInfo(WORKFLOW_ID);
        Assertions.assertEquals(JsonUtils.toJson(source.getRequest()), JsonUtils.toJson(target));
    }

    @Test
    public void testWorkFlowRun() {
        String WORKFLOW_ID = IDGenerator.newWorkflowId();
        Example<WorkflowSchema, Map> source = readJson("post-apps-{workflowId}-workflow-draft.json",
                new TypeReference<Example<WorkflowSchema, Map>>() {
                });
        dify.saveDraftInfo(WORKFLOW_ID, source.getRequest());
        WorkflowSchema target = dify.getDraftInfo(WORKFLOW_ID);
        Assertions.assertEquals(JsonUtils.toJson(source.getRequest()), JsonUtils.toJson(target));
        //todo 处理多线程导致的事务不可见问题
        /* Object response = dify.workflowRun(WORKFLOW_ID, WorkflowOps.WorkflowRun.builder()
                        .responseMode(WorkflowOps.ResponseMode.blocking.name())
                        .inputs(Maps.of("q", "我想要烧一个红烧肉").asMap())
                .build());
        Assertions.assertNotNull(response);*/

    }

    @Test
    public void testWorkFlowNodeRun() {
        String WORKFLOW_ID = IDGenerator.newWorkflowId();
        Example<WorkflowSchema, Map> source = readJson("post-apps-{workflowId}-workflow-draft.json",
                new TypeReference<Example<WorkflowSchema, Map>>() {
                });
        dify.saveDraftInfo(WORKFLOW_ID, source.getRequest());
        WorkflowSchema target = dify.getDraftInfo(WORKFLOW_ID);
        Assertions.assertEquals(JsonUtils.toJson(source.getRequest()), JsonUtils.toJson(target));

        Object response = dify.nodeRun(WORKFLOW_ID, "1716460032513",
                WorkflowOps.WorkflowNodeRun.builder().inputs(Maps.of("input", "我想要烧一个红烧肉").asMap()).build());
        Assertions.assertNotNull(response);

        DifyWorkflowRunStreamingCallback.DifyData data = (DifyWorkflowRunStreamingCallback.DifyData) response;
        Map nodeRunResult = data.getOutputs();
        Assertions.assertEquals(nodeRunResult.get("class_name"), "美食相关的问题");
    }

    @Test
    public void testWorkFlowStreamRun() {
        String WORKFLOW_ID = IDGenerator.newWorkflowId();
        Example<WorkflowSchema, Map> source = readJson("post-apps-{workflowId}-workflow-draft.json",
                new TypeReference<Example<WorkflowSchema, Map>>() {
                });
        dify.saveDraftInfo(WORKFLOW_ID, source.getRequest());
        WorkflowSchema target = dify.getDraftInfo(WORKFLOW_ID);
        Assertions.assertEquals(JsonUtils.toJson(source.getRequest()), JsonUtils.toJson(target));

        Object response = dify.workflowRun(WORKFLOW_ID, WorkflowOps.WorkflowRun.builder().responseMode(WorkflowOps.ResponseMode.streaming.name())
                .inputs(Maps.of("input", "我想要烧一个红烧肉").asMap()).build());
        Assertions.assertNotNull(response);

        ResponseBodyEmitter emitter = (ResponseBodyEmitter) response;
        Assertions.assertNotNull(emitter);
    }

    private <T, E> Example<T, E> readJson(String path, TypeReference<Example<T, E>> clazz) {
        String json = readResource(path);
        return JsonUtils.fromJson(json, clazz);
    }

    private String readResource(String path) {
        File file = new File("src/test/resources/" + path);
        try {
            return FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Example<T, E> {
        private T request;
        private E response;
    }
}
