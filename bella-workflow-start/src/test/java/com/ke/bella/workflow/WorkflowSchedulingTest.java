package com.ke.bella.workflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.api.WorkflowScheduleController;

@AutoConfigureMockMvc
public class WorkflowSchedulingTest extends AbstractTest {

    @Autowired
    WorkflowScheduleController controller;

    @Autowired
    WorkflowSchedulingHelper workflowSchedulingHelper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testWorkflowScheduling() throws Exception {
        WorkflowOps.WorkflowScheduling body = WorkflowOps.WorkflowScheduling.builder()
                .userId(userIdL)
                .userName("chenjiakai004")
                .tenantId("04633c4f-8638-43a3-a02e-af23c29f821f")
                .workflowId("55513295-ab05-4da7-b99b-bab26596ec9c")
                .inputs(Collections.singletonMap("demo", "demo"))
                .cronExpression("0 0/2 * * * ?")
                .build();
        mockMvc.perform(post("/v1/workflow/trigger/scheduling") // 替换为您的请求路径
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(body)))
                .andExpect(status().isOk());
    }
}
