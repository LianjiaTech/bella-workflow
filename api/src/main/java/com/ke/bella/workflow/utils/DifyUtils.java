package com.ke.bella.workflow.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.api.DifyController;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.node.NodeType;
import com.ke.bella.workflow.service.WorkflowRunCallback;

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

    public static List<DifyController.DifyNodeExecution.DifyNodeRun> transfer0(List<WorkflowRunCallback.WorkflowRunLog> workflowRunLogs) {
        AtomicInteger index = new AtomicInteger(1);
        AtomicReference<String> lastNodeId = new AtomicReference<>(null);
        List<DifyController.DifyNodeExecution.DifyNodeRun> collect = workflowRunLogs.stream()
                .sorted(Comparator.comparing(WorkflowRunCallback.WorkflowRunLog::getCtime))
                .map(nodeRunDB -> {
                    DifyController.DifyNodeExecution.DifyNodeRun nodeRun = createDifyNodeRun(nodeRunDB, index.getAndIncrement(), lastNodeId.get());
                    lastNodeId.set(nodeRunDB.getNodeId());
                    return nodeRun;
                })
                .collect(Collectors.toList());
        Collections.reverse(collect);
        return collect;
    }

    private static DifyController.DifyNodeExecution.DifyNodeRun createDifyNodeRun(WorkflowRunCallback.WorkflowRunLog runLog, int index,
            String predecessorNodeId) {
        Long ctime = Optional.ofNullable(runLog.getCtime()).map(c -> Instant.ofEpochMilli(c).atZone(ZoneId.systemDefault()).toEpochSecond())
                .orElse(0L);
        Long elapsedTime = Optional.ofNullable(runLog.getElapsedTime()).orElse(0L);
        return DifyController.DifyNodeExecution.DifyNodeRun.builder()
                .id(runLog.getNodeRunId())
                .index(index)
                .predecessor_node_id(predecessorNodeId)
                .node_id(runLog.getNodeId())
                .node_type(runLog.getNodeType())
                .title(runLog.getNodeTitle())
                .inputs((Map) runLog.getNodeInputs())
                .process_data((Map) runLog.getNodeProcessData())
                .outputs((Map) runLog.getNodeOutputs())
                .status(runLog.getStatus())
                .error(runLog.getError())
                .elapsed_time(elapsedTime / 1000d)
                .created_at(ctime)
                .created_by_role("account")
                .created_by_account(
                        DifyController.Account.builder().id(String.valueOf(runLog.getUserId())).name(runLog.getUserName()).email("").build())
                .finished_at((ctime + elapsedTime))
                .build();
    }

    public static DifyController.DifyRunHistory transfer(WorkflowRunCallback.WorkflowRunLog e) {
        Long ctime = Optional.ofNullable(e.getCtime()).map(c -> Instant.ofEpochMilli(c).atZone(ZoneId.systemDefault()).toEpochSecond())
                .orElse(0L);
        Long elapsedTime = Optional.ofNullable(e.getElapsedTime()).orElse(0L);

        return DifyController.DifyRunHistory.builder()
                .id(e.getWorkflowRunId())
                .conversation_id(String.valueOf(e.getThreadId()))
                .status(e.getStatus())
                .created_by_account(DifyController.Account.builder().id(String.valueOf(e.getUserId())).name(e.getUserName()).email("").build())
                .created_at(ctime - elapsedTime)
                .finished_at(ctime)
                .elapsed_time(elapsedTime / 1000d)
                .build();
    }

    @SuppressWarnings("all")
    public static DifyController.DifyRunHistoryDetails transfer(WorkflowRunCallback.WorkflowRunLog wrl, WorkflowDB wf) {
        WorkflowSchema workflowSchema = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
        Map difyInputs = new HashMap();
        difyInputs.put("inputs", wrl.getInputs());
        difyInputs.put("sys", wrl.getSys());

        return DifyController.DifyRunHistoryDetails.builder()
                .id(wrl.getWorkflowRunId())
                .version(wf.getVersion() == 0 ? "draft" : String.valueOf(wf.getVersion()))
                .status(wrl.getStatus())
                .created_by_account(
                        DifyController.Account.builder().id(String.valueOf(wrl.getUserId())).name(wrl.getUserName()).email("").build())
                .created_at(wrl.getCtime())
                .finished_at(wrl.getCtime() + wrl.getElapsedTime())
                .elapsed_time(wrl.getElapsedTime() / 1000d)
                .graph(workflowSchema.getGraph())
                .inputs(difyInputs).build();
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
                .releaseDescription(db.getReleaseDescription())
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
