package com.ke.bella.workflow.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.api.DifyController;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.node.NodeType;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DifyUtils {
    public static WorkflowSchema getDefaultWorkflowSchema() {
        WorkflowSchema.Graph graph = new WorkflowSchema.Graph();
        Map<String, Object> maps = Maps.newHashMap();
        maps.put("type", NodeType.START.name);
        maps.put("title", "开始");
        maps.put("variables", Lists.newArrayList());
        maps.put("selected", true);

        graph.setNodes(Lists.newArrayList(WorkflowSchema.Node.builder()
                .id(System.currentTimeMillis() + "")
                .data(maps)
                .width(244)
                .height(54)
                .position(WorkflowSchema.Position.builder()
                        .x(100)
                        .y(100)
                        .build())
                .positionAbsolute(WorkflowSchema.Position.builder()
                        .x(100)
                        .y(100)
                        .build())
                .targetPosition("left")
                .sourcePosition("right")
                .type("custom")
                .build()));
        graph.setViewport(WorkflowSchema.Viewport.builder()
                .zoom(1.0)
                .x(80)
                .y(126)
                .build());
        graph.setEdges(Lists.newArrayList());

        WorkflowSchema schema = new WorkflowSchema();

        schema.setGraph(graph);
        return schema;
    }

    public static List<DifyController.DifyNodeExecution.DifyNodeRun> transfer(List<WorkflowNodeRunDB> nodeRunDBs) {
        AtomicInteger index = new AtomicInteger(1);
        AtomicReference<String> lastNodeId = new AtomicReference<>(null);
        List<DifyController.DifyNodeExecution.DifyNodeRun> collect = nodeRunDBs.stream()
                .sorted(Comparator.comparing(WorkflowNodeRunDB::getCtime))
                .map(nodeRunDB -> {
                    DifyController.DifyNodeExecution.DifyNodeRun nodeRun = createDifyNodeRun(nodeRunDB, index.getAndIncrement(), lastNodeId.get());
                    lastNodeId.set(nodeRunDB.getNodeId());
                    return nodeRun;
                })
                .collect(Collectors.toList());
        Collections.reverse(collect);
        return collect;
    }

    private static DifyController.DifyNodeExecution.DifyNodeRun createDifyNodeRun(WorkflowNodeRunDB nodeRunDB, int index, String predecessorNodeId) {
        return DifyController.DifyNodeExecution.DifyNodeRun.builder()
                .id(nodeRunDB.getNodeRunId())
                .index(index)
                .predecessor_node_id(predecessorNodeId)
                .node_id(nodeRunDB.getNodeId())
                .node_type(nodeRunDB.getNodeType())
                .title(nodeRunDB.getTitle())
                .inputs(JsonUtils.fromJson(nodeRunDB.getInputs(), Map.class))
                .process_data(JsonUtils.fromJson(nodeRunDB.getProcessData(), Map.class))
                .outputs(JsonUtils.fromJson(nodeRunDB.getOutputs(), Map.class))
                .status(nodeRunDB.getStatus())
                .error(nodeRunDB.getError())
                .elapsed_time(nodeRunDB.getElapsedTime() / 1000d)
                .created_at(nodeRunDB.getCtime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .created_by_role("account")
                .created_by_account(DifyController.Account.builder().id(String.valueOf(nodeRunDB.getCuid())).name(nodeRunDB.getCuName()).email("").build())
                .finished_at(nodeRunDB.getMtime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .build();
    }

    public static DifyController.DifyRunHistory transfer(WorkflowRunDB e) {
        return DifyController.DifyRunHistory.builder()
                .id(e.getWorkflowRunId())
                .version(String.valueOf(e.getWorkflowVersion()))
                .conversation_id(String.valueOf(e.getThreadId()))
                .status(e.getStatus())
                .created_by_account(DifyController.Account.builder().id(String.valueOf(e.getCuid())).name(e.getCuName()).email("").build())
                .created_at(e.getCtime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .finished_at(e.getMtime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .elapsed_time(e.getElapsedTime() / 1000d)
                .build();
    }

    public static DifyController.DifyRunHistoryDetails transfer(WorkflowRunDB wr, WorkflowDB wf) {
        WorkflowSchema workflowSchema = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
        return DifyController.DifyRunHistoryDetails.builder()
                .id(wr.getWorkflowRunId())
                .version(wr.getWorkflowVersion() == 0 ? "draft" : String.valueOf(wr.getWorkflowVersion()))
                .status(wr.getStatus())
                .created_by_account(
                        DifyController.Account.builder().id(String.valueOf(wr.getCuid())).name(wr.getCuName()).email("").build())
                .created_at(wr.getCtime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .finished_at(wr.getMtime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .elapsed_time(wr.getElapsedTime() / 1000d)
                .graph(workflowSchema.getGraph())
                .inputs(JsonUtils.fromJson(wr.getInputs(), Map.class)).build();
    }

    public static DifyController.DifyWorkflowVersion transfer(WorkflowDB db) {
        return DifyController.DifyWorkflowVersion.builder()
                .id(db.getId())
                .tenantId(db.getTenantId())
                .workflowId(db.getWorkflowId())
                .title(db.getTitle())
                .mode(db.getMode())
                .desc(db.getDesc())
                .version(db.getVersion())
                .cuid(db.getCuid())
                .cuName(db.getCuName())
                .ctime(db.getCtime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .muid(db.getMuid())
                .muName(db.getMuName())
                .mtime(db.getMtime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .graph(JsonUtils.fromJson(db.getGraph(), WorkflowSchema.class).getGraph())
                .build();
    }
}
