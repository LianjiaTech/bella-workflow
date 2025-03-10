package com.ke.bella.workflow.dify;

import static com.ke.bella.openapi.BellaContext.BELLA_TRACE_HEADER;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.andrewoma.dexx.collection.Maps;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.Operator;
import com.ke.bella.openapi.apikey.ApikeyInfo;
import com.ke.bella.workflow.AbstractTest;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.api.DifyController;
import com.ke.bella.workflow.api.DifyController.DifyWorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.api.callbacks.DifyWorkflowRunStreamingCallback;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("rawtypes")
public class DifyControllerTest extends AbstractTest {

    @Autowired
    private DifyController dify;

    @BeforeEach
    public void initContext() {
        BellaContext.setOperator(Operator.builder().userId(userIdL).tenantId("test").userName("test").build());
        BellaContext.setApikey(ApikeyInfo.builder().apikey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8").build());
        BellaContext.getHeaders().put(BELLA_TRACE_HEADER, BellaContext.generateTraceId("workflow"));
    }

    @AfterEach
    public void clearContext() {
        BellaContext.clearAll();
    }

    @Test
    public void testCreateDraft() {
        String WORKFLOW_ID = IDGenerator.newWorkflowId();
        Example<WorkflowSchema, Map> source = readJson("post-apps-workflowid-workflow-draft.json",
                new TypeReference<Example<WorkflowSchema, Map>>() {
                });
        dify.saveDraftInfo(WORKFLOW_ID, source.getRequest(), null);
        WorkflowSchema target = dify.getDraftInfo(WORKFLOW_ID);
        Assertions.assertEquals(JsonUtils.toJson(source.getRequest()), JsonUtils.toJson(target));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWorkFlowRun() {
        String WORKFLOW_ID = IDGenerator.newWorkflowId();
        Example<WorkflowSchema, Map> source = readJson("post-apps-workflowid-workflow-draft.json",
                new TypeReference<Example<WorkflowSchema, Map>>() {
                });
        dify.saveDraftInfo(WORKFLOW_ID, source.getRequest(), null);
        WorkflowSchema target = dify.getDraftInfo(WORKFLOW_ID);
        Assertions.assertEquals(JsonUtils.toJson(source.getRequest()), JsonUtils.toJson(target));

        Object response = dify.workflowRun(WORKFLOW_ID, DifyWorkflowRun.builder()
                .responseMode(WorkflowOps.ResponseMode.blocking.name())
                .inputs(Maps.of("#1715941054541.q#", "我想要烧一个红烧肉", "q", "mock").asMap())
                .build());
        Map<String, Object> callback = (Map<String, Object>) response;
        Assertions.assertNotNull(callback);
        Assertions.assertEquals(callback.get("status"), "succeeded", JsonUtils.toJson(callback));

    }

    @Test
    public void testWorkFlowNodeRun() {
        String WORKFLOW_ID = IDGenerator.newWorkflowId();
        Example<WorkflowSchema, Map> source = readJson("post-apps-workflowid-workflow-draft.json",
                new TypeReference<Example<WorkflowSchema, Map>>() {
                });
        dify.saveDraftInfo(WORKFLOW_ID, source.getRequest(), null);
        WorkflowSchema target = dify.getDraftInfo(WORKFLOW_ID);
        Assertions.assertEquals(JsonUtils.toJson(source.getRequest()), JsonUtils.toJson(target));

        Object response = dify.nodeRun(WORKFLOW_ID, "1716460032513",
                WorkflowOps.WorkflowNodeRun.builder().inputs(Maps.of("#1715941054541.q#", "我想要烧一个红烧肉").asMap()).build());
        Assertions.assertNotNull(response);

        DifyWorkflowRunStreamingCallback.DifyData data = (DifyWorkflowRunStreamingCallback.DifyData) response;
        Map nodeRunResult = data.getOutputs();
        Assertions.assertEquals(nodeRunResult.get("class_name"), "美食相关的问题");
    }

    @Test
    public void testWorkFlowStreamRun() {
        String WORKFLOW_ID = IDGenerator.newWorkflowId();
        Example<WorkflowSchema, Map> source = readJson("post-apps-workflowid-workflow-draft.json",
                new TypeReference<Example<WorkflowSchema, Map>>() {
                });
        dify.saveDraftInfo(WORKFLOW_ID, source.getRequest(), null);
        WorkflowSchema target = dify.getDraftInfo(WORKFLOW_ID);
        Assertions.assertEquals(JsonUtils.toJson(source.getRequest()), JsonUtils.toJson(target));

        Object response = dify.workflowRun(WORKFLOW_ID, DifyWorkflowRun.builder().responseMode(WorkflowOps.ResponseMode.streaming.name())
                .inputs(Maps.of("#1715941054541.q#", "我想要烧一个红烧肉").asMap()).build());
        Assertions.assertNotNull(response);

        ResponseBodyEmitter emitter = (ResponseBodyEmitter) response;
        Assertions.assertNotNull(emitter);
    }

    private <T, E> Example<T, E> readJson(String path, TypeReference<Example<T, E>> clazz) {
        String json = readResource(path);
        return JsonUtils.fromJson(json, clazz);
    }

    private String readResource(String path) {
        try {
            // 获取 测试资源文件
            File file = new File(Thread.currentThread()
                    .getContextClassLoader()
                    .getResource(path).getFile());
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
