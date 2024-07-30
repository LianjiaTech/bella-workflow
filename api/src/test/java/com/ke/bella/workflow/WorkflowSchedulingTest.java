package com.ke.bella.workflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.api.BellaResponse;
import com.ke.bella.workflow.api.TriggerController;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.trigger.WorkflowScheduler;
import com.ke.bella.workflow.trigger.WorkflowSchedulingStatus;
import com.ke.bella.workflow.utils.JsonUtils;

@AutoConfigureMockMvc
public class WorkflowSchedulingTest extends AbstractTest {

    @Autowired
    TriggerController controller;

    @Autowired
    WorkflowScheduler workflowSchedulingHelper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testWorkflowScheduling() throws Exception {
        // 1. 创建scheduling 任务
        WorkflowOps.WorkflowScheduling body = WorkflowOps.WorkflowScheduling.builder()
                .userId(userIdL)
                .userName("chenjiakai004")
                .tenantId("04633c4f-8638-43a3-a02e-af23c29f821f")
                .workflowId("55513295-ab05-4da7-b99b-bab26596ec9c")
                .inputs(Collections.singletonMap("demo", "demo"))
                .cronExpression("0 0/2 * * * ?")
                .build();
        MvcResult schedulingResult = mockMvc.perform(post("/v1/workflow/trigger/scheduling/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(body)))
                .andExpect(status().isOk()).andReturn();
        MockHttpServletResponse schedulingResp = schedulingResult.getResponse();
        BellaResponse<WorkflowSchedulingDB> wfs = JsonUtils.fromJson(schedulingResp.getContentAsString(),
                new TypeReference<BellaResponse<WorkflowSchedulingDB>>() {
                });

        // 2. 查询scheduling 任务
        WorkflowOps.WorkflowSchedulingPage page = WorkflowOps.WorkflowSchedulingPage.builder()
                .tenantId("04633c4f-8638-43a3-a02e-af23c29f821f")
                .workflowSchedulingId(wfs.getData().getWorkflowSchedulingId())
                .build();
        MvcResult pageResult = mockMvc.perform(post("/v1/workflow/trigger/scheduling/page")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(page)))
                .andExpect(status().isOk()).andReturn();
        MockHttpServletResponse pageResp = pageResult.getResponse();
        BellaResponse<Page<WorkflowSchedulingDB>> wfsPage = JsonUtils.fromJson(pageResp.getContentAsString(),
                new TypeReference<BellaResponse<Page<WorkflowSchedulingDB>>>() {
                });
        Assertions.assertNotNull(wfsPage.getData().getData());

        // 3. 测试停止一个scheduling任务
        WorkflowOps.WorkflowSchedulingOp wlsOp = WorkflowOps.WorkflowSchedulingOp.builder()
                .tenantId("04633c4f-8638-43a3-a02e-af23c29f821f")
                .workflowSchedulingId(wfs.getData().getWorkflowSchedulingId())
                .build();
        MvcResult stopResult = mockMvc.perform(post("/v1/workflow/trigger/scheduling/stop")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(wlsOp)))
                .andExpect(status().isOk()).andReturn();
        MockHttpServletResponse stopResp = stopResult.getResponse();
        BellaResponse<WorkflowSchedulingDB> stopWfs = JsonUtils.fromJson(stopResp.getContentAsString(),
                new TypeReference<BellaResponse<WorkflowSchedulingDB>>() {
                });
        Assertions.assertEquals(WorkflowSchedulingStatus.stopped.name(), stopWfs.getData().getStatus());

        // 4. 测试重新启动
        MvcResult startResult = mockMvc.perform(post("/v1/workflow/trigger/scheduling/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(wlsOp)))
                .andExpect(status().isOk()).andReturn();
        MockHttpServletResponse startResp = startResult.getResponse();
        BellaResponse<WorkflowSchedulingDB> startWfs = JsonUtils.fromJson(startResp.getContentAsString(),
                new TypeReference<BellaResponse<WorkflowSchedulingDB>>() {
                });
        Assertions.assertEquals(WorkflowSchedulingStatus.running.name(), startWfs.getData().getStatus());

        // 5. 手动触发
        mockMvc.perform(post("/v1/workflow/trigger/scheduling/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(wlsOp)))
                .andExpect(status().is(201));
        // 6. 查看workflow-runs不为空 fixme 此处因为测试库和junit库不一致先不实现
    }
}
