package com.ke.bella.workflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;

import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.JsonUtils;

public abstract class CommonNodeTest {

    @BeforeAll
    public static void initBellaContext() {
        Configs.API_BASE = "https://example.com/v1/";
        Configs.OPEN_API_BASE = "https://example.com/v1/";
        Configs.BELLA_TOOL_API_BASE = "http://example.com";
        Configs.BORE_API_BASE = "https://example.com";
        Configs.SAND_BOX_API_BASE = "https://example.com/v1/";
        Configs.TASK_THREAD_NUMS = 100;
        BellaContext.setOperator(Operator.builder().userId(userIdL).tenantId("test").userName("test").build());
        BellaContext.setApiKey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8");
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
