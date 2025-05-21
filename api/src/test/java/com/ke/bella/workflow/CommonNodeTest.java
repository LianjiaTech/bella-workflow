package com.ke.bella.workflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.utils.JsonUtils;

public abstract class CommonNodeTest {

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
