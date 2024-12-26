package com.ke.bella.workflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.ke.bella.openapi.apikey.ApikeyInfo;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.Operator;
import org.junit.jupiter.api.BeforeAll;

import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;

import static com.ke.bella.openapi.BellaContext.BELLA_TRACE_HEADER;

public abstract class CommonNodeTest {

    @BeforeEach
    public void initBellaContext() {
        Configs.API_BASE = "https://example.com/v1/";
        Configs.OPEN_API_BASE = "https://example.com/v1/";
        Configs.BELLA_TOOL_API_BASE = "http://example.com";
        Configs.BORE_API_BASE = "https://example.com";
        Configs.SAND_BOX_API_BASE = "https://example.com/v2/";
        Configs.TASK_THREAD_NUMS = 100;
        BellaContext.setOperator(Operator.builder().userId(userIdL).tenantId("test").userName("test").build());
        BellaContext.setApikey(ApikeyInfo.builder().apikey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8").build());
        BellaContext.getHeaders().put(BELLA_TRACE_HEADER, BellaContext.generateTraceId("workflow"));
    }

    public static WorkflowContext createContext(String caseFilePath, Map<String, Object> inputs) throws IOException {
        // 读取JSON文件并将其解析为JsonNode
        WorkflowSchema meta = JsonUtils.fromJson(new String(Files.readAllBytes(Paths.get(caseFilePath))),
                WorkflowSchema.class);
        WorkflowGraph graph = new WorkflowGraph(meta);
        WorkflowContext context = WorkflowContext.builder()
                .graph(graph)
                .state(new WorkflowRunState())
                .userInputs(inputs)
                .runId(IDGenerator.newWorkflowRunId())
                .build();
        return context;
    }
}
