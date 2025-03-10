package com.ke.bella.workflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.Operator;
import com.ke.bella.workflow.api.BellaResponse;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.utils.JsonUtils;

@AutoConfigureMockMvc
public class WorkflowServiceTest extends AbstractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkflowService ws;

    private static final String TENANT_ID = "04633c4f-8638-43a3-a02e-af23c29f821f";
    private static final Long UCID = userIdL;
    private static final String USER_NAME = "userName";

    @Test
    @Transactional
    public void testTemplate() throws Exception {
        // 1. 准备测试数据
        Page<WorkflowDB> db = pageWorkflow();
        Assertions.assertNotNull(db);
        Assertions.assertFalse(CollectionUtils.isEmpty(db.getData()));

        WorkflowDB toTemplateWorkflow = db.getData().get(0);

        Page<WorkflowTemplate> pageBeforeCreateTemplate = testPageWorkflowTemplates();

        Assertions.assertNotNull(pageBeforeCreateTemplate);
        // 1.1. 创建template前没有这个workflowId的template
        Assertions
                .assertTrue(pageBeforeCreateTemplate.getData().stream().noneMatch(t -> t.getWorkflowId().equals(toTemplateWorkflow.getWorkflowId())));

        // 2. 发布为模板
        WorkflowTemplate beforeCopied = testPublishAsTemplate(toTemplateWorkflow.getWorkflowId(), toTemplateWorkflow.getVersion());

        Page<WorkflowTemplate> pageAfterCreateTemplate = testPageWorkflowTemplates();
        // 2.1. 创建template前有这个workflowId的template
        Assertions.assertTrue(pageAfterCreateTemplate.getData().stream().anyMatch(t -> t.getWorkflowId().equals(toTemplateWorkflow.getWorkflowId())));

        // 2.2. 能够get
        testGetWorkflowTemplate(beforeCopied.getTemplateId());

        WorkflowDB workflowDB = testNewWorkflowFromTemplate(beforeCopied.getTemplateId());
        Assertions.assertNotNull(workflowDB);

        // 2.3. 拷贝次数递增
        WorkflowTemplate afterCopied = testGetWorkflowTemplate(beforeCopied.getTemplateId());
        Assertions.assertEquals(afterCopied.getCopies().longValue(), beforeCopied.getCopies() + 1);
    }

    private Page<WorkflowDB> pageWorkflow() {
        BellaContext.setOperator(Operator.builder()
                .tenantId(TENANT_ID)
                .userId(UCID)
                .userName(USER_NAME)
                .build());
        Page<WorkflowDB> db = ws.pageWorkflows(WorkflowOps.WorkflowPage.builder()
                .page(1)
                .pageSize(1)
                .userId(UCID)
                .userName(USER_NAME)
                .tenantId(TENANT_ID)
                .build());
        return db;
    }

    private Page<WorkflowTemplate> testPageWorkflowTemplates() throws Exception {
        // 1. 创建测试数据
        WorkflowOps.WorkflowPage page = WorkflowOps.WorkflowPage.builder()
                .page(1)
                .pageSize(10)
                .tenantId(TENANT_ID)
                .userId(UCID)
                .userName(USER_NAME)
                .build();

        // 2. 执行分页查询
        MvcResult pageResult = mockMvc.perform(post("/v1/workflow/templates/page")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(page)))
                .andExpect(status().isOk())
                .andReturn();

        // 3. 验证结果
        MockHttpServletResponse pageResp = pageResult.getResponse();
        BellaResponse<Page<WorkflowTemplate>> templatePage = JsonUtils.fromJson(
                pageResp.getContentAsString(),
                new TypeReference<BellaResponse<Page<WorkflowTemplate>>>() {
                });

        Assertions.assertNotNull(templatePage);

        return templatePage.getData();
    }

    private WorkflowTemplate testGetWorkflowTemplate(String templateId) throws Exception {
        BellaContext.setOperator(Operator.builder()
                .tenantId(TENANT_ID)
                .userId(UCID)
                .userName(USER_NAME)
                .build());
        // 1. 准备测试数据
        WorkflowOps.WorkflowSync sync = WorkflowOps.WorkflowSync.builder()
                .templateId(templateId)
                .tenantId(TENANT_ID)
                .userId(UCID)
                .userName(USER_NAME)
                .build();

        // 2. 获取模板
        WorkflowTemplate template = ws.getWorkflowTemplate(sync);

        Assertions.assertNotNull(template);
        Assertions.assertEquals(templateId, template.getTemplateId());

        return template;
    }

    private WorkflowDB testNewWorkflowFromTemplate(String templateId) throws Exception {
        WorkflowOps.WorkflowSync sync = WorkflowOps.WorkflowSync.builder()
                .templateId(templateId)
                .tenantId(TENANT_ID)
                .userId(UCID)
                .userName(USER_NAME)
                .build();

        // 2. 从模板创建工作流
        MvcResult result = mockMvc.perform(post("/v1/workflow/create/from-template")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(sync)))
                .andExpect(status().isOk())
                .andReturn();

        // 3. 验证结果
        MockHttpServletResponse response = result.getResponse();
        BellaResponse<WorkflowDB> workflow = JsonUtils.fromJson(
                response.getContentAsString(),
                new TypeReference<BellaResponse<WorkflowDB>>() {
                });

        Assertions.assertNotNull(workflow);
        return workflow.getData();
    }

    private WorkflowTemplate testPublishAsTemplate(String workflowId, Long version) throws Exception {
        // 1. 准备测试数据
        WorkflowOps.WorkflowOp op = WorkflowOps.WorkflowOp.builder()
                .workflowId(workflowId)
                .tenantId(TENANT_ID)
                .userId(UCID)
                .userName(USER_NAME)
                .version(version)
                .build();

        // 2. 发布为模板
        MvcResult result = mockMvc.perform(post("/v1/workflow/publish-as-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(op)))
                .andExpect(status().isOk())
                .andReturn();

        // 3. 验证结果
        MockHttpServletResponse response = result.getResponse();
        BellaResponse<WorkflowTemplate> template = JsonUtils.fromJson(
                response.getContentAsString(),
                new TypeReference<BellaResponse<WorkflowTemplate>>() {
                });

        Assertions.assertNotNull(template);
        return template.getData();
    }
}
