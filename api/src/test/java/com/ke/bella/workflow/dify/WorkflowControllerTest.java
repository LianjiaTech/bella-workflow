package com.ke.bella.workflow.dify;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ke.bella.workflow.AbstractTest;
import com.ke.bella.workflow.service.JsonUtils;
import com.ke.bella.workflow.api.BellaResponse;
import com.ke.bella.workflow.api.WorkflowController;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.service.WorkflowService;

@SuppressWarnings("rawtypes")
@Transactional
@AutoConfigureMockMvc
public class WorkflowControllerTest extends AbstractTest {

    private static final String BELLA_TENANT_ID = "04633c4f-8638-43a3-a02e-af23c29f821f";
    private static final Long MOCK_USER_ID = 1000000029361137L;
    private static final String originWorkflowId = "WKFL-d5c41f2c-8456-4d21-ae95-de16b15f8164";
    private static final String MOCK_USER_NAME = "mock";
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // 注册JavaTimeModule以处理LocalDateTime
        mapper.registerModule(new JavaTimeModule());
    }

    @Autowired
    WorkflowController workflowController;;
    @Autowired
    WorkflowService workflowService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCopyWorkflow() throws Exception {
        // query
        WorkflowOps.WorkflowOp workflowOp = new WorkflowOps.WorkflowOp();
        workflowOp.setTenantId(BELLA_TENANT_ID);
        workflowOp.setUserId(MOCK_USER_ID);
        workflowOp.setUserName(MOCK_USER_NAME);
        workflowOp.setWorkflowId(originWorkflowId);
        MvcResult getReturn = mockMvc.perform(post("/v1/workflow/draft/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(workflowOp)))
                .andExpect(status().isOk()).andReturn();
        MockHttpServletResponse response = getReturn.getResponse();
        BellaResponse<WorkflowDB> result = mapper.readValue(response.getContentAsString(), new TypeReference<BellaResponse<WorkflowDB>>() {
        });
        WorkflowDB origin = result.getData();

        WorkflowOps.WorkflowCopy workflowCopy = new WorkflowOps.WorkflowCopy();
        workflowCopy.setWorkflowId(originWorkflowId);
        workflowCopy.setUserId(MOCK_USER_ID);
        workflowCopy.setUserName(MOCK_USER_NAME);
        workflowCopy.setTenantId(BELLA_TENANT_ID);
        workflowCopy.setVersion(origin.getVersion());

        // to copy
        MvcResult copyReturn = mockMvc.perform(post("/v1/workflow/copy") // 替换为您的请求路径
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(workflowCopy)))
                .andExpect(status().isOk()).andReturn();
        MockHttpServletResponse copyResp = copyReturn.getResponse();
        BellaResponse<WorkflowDB> copyResult = mapper.readValue(copyResp.getContentAsString(), new TypeReference<BellaResponse<WorkflowDB>>() {
        });
        WorkflowDB copied = copyResult.getData();
        Assertions.assertNotNull(copied);
        Assertions.assertEquals(copied.getGraph(), origin.getGraph());
        Assertions.assertNotEquals(copied.getWorkflowId(), origin.getWorkflowId());
    }
}
