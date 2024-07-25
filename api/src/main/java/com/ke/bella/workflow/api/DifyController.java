package com.ke.bella.workflow.api;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ke.bella.workflow.IWorkflowCallback.File;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.api.WorkflowOps.ResponseMode;
import com.ke.bella.workflow.api.WorkflowOps.TriggerFrom;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowOp;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.api.callbacks.DifySingleNodeRunBlockingCallback;
import com.ke.bella.workflow.api.callbacks.DifyWorkflowRunStreamingCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunBlockingCallback;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.node.NodeType;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@RestController
@RequestMapping("/console/api/apps")
public class DifyController {

    @Autowired
    WorkflowService ws;

    @Value("${bella.open.api.key}")
    private String openApiKey;

    private void initContext() {
        if(Objects.isNull(BellaContext.getOperator()) ||
                !StringUtils.hasText(BellaContext.getOperator().getTenantId()) ||
                !StringUtils.hasText(BellaContext.getOperator().getUserName()) ||
                Objects.isNull(BellaContext.getOperator().getUserId())) {
            BellaContext.setOperator(Operator.builder()
                    .userId(userIdL)
                    .tenantId("test")
                    .userName("test")
                    .build());
        }
        BellaContext.setApiKey(openApiKey);
    }

    @GetMapping
    public Page<DifyApp> pageApps(@RequestParam int page, @RequestParam int limit, @RequestParam String name) {
        initContext();

        WorkflowPage op = WorkflowPage.builder().page(page).pageSize(limit).name(name).build();

        Page<WorkflowDB> wfs = ws.pageDraftWorkflow(op);

        List<DifyApp> apps = new ArrayList<>();
        wfs.getData().forEach(wf -> apps.add(DifyApp.builder()
                .tenantId(wf.getTenantId())
                .id(wf.getWorkflowId())
                .name(wf.getTitle())
                .description(wf.getDesc())
                .mode(wf.getMode())
                .api_base_url(Configs.API_BASE)
                .build()));

        Page<DifyApp> ret = new Page<>();
        ret.page(page);
        ret.pageSize(wfs.getPageSize());
        ret.total(wfs.getTotal());
        ret.list(apps);
        return ret;
    }

    @PostMapping
    public DifyApp createApp(@RequestBody DifyApp app) {
        initContext();

        WorkflowSchema schema = getDefaultWorkflowSchema();
        WorkflowSync sync = WorkflowSync.builder()
                .title(app.getName())
                .desc(app.getDescription())
                .graph(JsonUtils.toJson(schema))
                .mode(app.getMode())
                .build();
        WorkflowDB wf = ws.newWorkflow(sync);
        app.setId(wf.getWorkflowId());
        return app;
    }

    @GetMapping("/{workflowId}/workflows/draft")
    public WorkflowSchema getDraftInfo(@PathVariable String workflowId) {
        initContext();
        Assert.hasText(workflowId, "workflowId不能为空");
        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        if(Objects.isNull(wf)) {
            return getDefaultWorkflowSchema();
        }
        return JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
    }

    @GetMapping("/{workflowId}/workflows")
    public Page<WorkflowDB> getWorkflows(@PathVariable String workflowId, @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        initContext();
        Assert.hasText(workflowId, "workflowId不能为空");
        WorkflowPage op = WorkflowPage.builder().page(page).pageSize(limit).workflowId(workflowId).build();
        return ws.pageWorkflows(op);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DifyApp {
        String tenantId;
        String id;
        String name;
        String description;
        @Builder.Default
        String mode = "advanced-chat";
        @Builder.Default
        String icon = "\ud83e\udd16";
        @Builder.Default
        String icon_background = "#FFEAD5";
        boolean enable_site;
        @Builder.Default
        boolean enable_api = true;
        Object model_config;
        @Builder.Default
        Site site = new Site();
        String api_base_url;
        int created_at;
        @Builder.Default
        Object[] deleted_tools = new Object[0];
        @Builder.Default
        Object[] tags = new Object[0];

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Site {
            String access_token;
            String code;
            String title;
            Object icon;
            Object icon_background;
            Object description;
            String default_language;
            Object customize_domain;
            Object copyright;
            Object privacy_policy;
            Object custom_disclaimer;
            String customize_token_strategy;
            boolean prompt_public;
            String app_base_url;
        }
    }

    @GetMapping("/{workflowId}")
    public DifyApp getDifyApp(@PathVariable String workflowId) {
        initContext();
        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        return DifyApp.builder()
                .tenantId(wf.getTenantId())
                .id(workflowId)
                .name(wf.getTitle())
                .description(wf.getDesc())
                .mode(wf.getMode())
                .api_base_url(Configs.API_BASE)
                .build();
    }

    @PutMapping("/{workflowId}")
    public DifyApp updateDifyApp(@PathVariable String workflowId, @RequestBody DifyApp app) {
        initContext();

        WorkflowSync op = WorkflowSync.builder()
                .title(app.getName())
                .desc(app.getDescription())
                .workflowId(workflowId)
                .build();
        ws.syncWorkflow(op);

        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        return DifyApp.builder()
                .tenantId(wf.getTenantId())
                .id(workflowId)
                .mode(wf.getMode())
                .name(wf.getTitle())
                .description(wf.getDesc())
                .mode(wf.getMode())
                .api_base_url(Configs.API_BASE)
                .build();
    }

    @PostMapping("/{workflowId}/workflows/draft")
    public WorkflowSchema saveDraftInfo(@PathVariable String workflowId, @RequestBody WorkflowSchema schema) {
        initContext();
        Assert.hasText(workflowId, "workflowId不能为空");
        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        WorkflowSync sync = WorkflowSync.builder()
                .graph(Objects.nonNull(schema) ? JsonUtils.toJson(schema) : JsonUtils.toJson(getDefaultWorkflowSchema()))
                .workflowId(workflowId)
                .build();
        if(Objects.isNull(wf)) {
            ws.newWorkflow(sync);
        } else {
            ws.syncWorkflow(sync);
        }
        wf = ws.getDraftWorkflow(workflowId);
        return JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
    }

    @PostMapping("/{workflowId}/workflows/draft/nodes/{nodeId}/run")
    public Object nodeRun(@PathVariable String workflowId, @PathVariable String nodeId, @RequestBody WorkflowOps.WorkflowNodeRun op) {
        initContext();
        op.setWorkflowId(workflowId);
        Assert.hasText(workflowId, "workflowId不能为空");
        Assert.hasText(nodeId, "nodeId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");

        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        Assert.notNull(wf, String.format("工作流当前无draft版本，无法单独调试节点", op.workflowId));

        WorkflowRun op2 = WorkflowRun.builder()
                .userId(op.getUserId())
                .userName(op.getUserName())
                .tenantId(op.getTenantId())
                .workflowId(op.getWorkflowId())
                .inputs(op.getInputs())
                .responseMode(op.getResponseMode())
                .triggerFrom(WorkflowOps.TriggerFrom.DEBUG_NODE.name())
                .build();
        WorkflowRunDB wr = ws.newWorkflowRun(wf, op2);

        DifySingleNodeRunBlockingCallback callback = new DifySingleNodeRunBlockingCallback();
        ws.runNode(wr, nodeId, op.inputs, callback);
        return callback.getWorkflowNodeRunResult();
    }

    @GetMapping("/{workflowId}/workflows/publish")
    public Object getPublish(@PathVariable String workflowId) {
        initContext();
        Assert.hasText(workflowId, "workflowId不能为空");
        WorkflowDB wf = ws.getPublishedWorkflow(workflowId, null);
        if(Objects.isNull(wf)) {
            return getDefaultWorkflowSchema();
        }
        return JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
    }

    @PostMapping("/{workflowId}/workflows/publish")
    public Object publish(@PathVariable String workflowId) {
        initContext();
        Assert.hasText(workflowId, "workflowId不能为空");
        try {
            ws.publish(workflowId);
        } catch (Exception e) {
            return DifyResponse.builder().code(400).message(e.getMessage()).status("invalid_param").build();
        }
        return DifyResponse.builder().code(200).message("发布成功").status("success").build();
    }

    @PostMapping({ "/{workflowId}/workflows/draft/run", "/{workflowId}/advanced-chat/workflows/draft/run" })
    public Object workflowRun(@PathVariable String workflowId, @RequestBody DifyWorkflowRun op) {
        initContext();
        op.setWorkflowId(workflowId);
        WorkflowOps.ResponseMode mode = WorkflowOps.ResponseMode.valueOf(op.responseMode);
        Assert.hasText(workflowId, "workflowId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");

        WorkflowRun op2 = WorkflowRun.builder()
                .userId(op.getUserId())
                .userName(op.getUserName())
                .tenantId(op.getTenantId())
                .workflowId(op.getWorkflowId())
                .inputs(op.getInputs())
                .responseMode(op.getResponseMode())
                .triggerFrom(op.triggerFrom)
                .threadId(op.threadId)
                .query(op.query)
                .files(op.files)
                .build();

        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        Assert.notNull(wf, String.format("工作流[%s]当前无draft版本，无法调试", op2.workflowId));

        WorkflowRunDB wr = ws.newWorkflowRun(wf, op2);
        if(mode == WorkflowOps.ResponseMode.blocking) {
            WorkflowRunBlockingCallback callback = new WorkflowRunBlockingCallback(ws, 300000L);
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op2, callback));
            return callback.getWorkflowRunResult();
        } else {
            SseEmitter emitter = SseHelper.createSse(300000L, wr.getWorkflowRunId());
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op2, new DifyWorkflowRunStreamingCallback(emitter)));
            return emitter;
        }
    }

    @RequestMapping("/{workflowId}/workflow-app-logs")
    public Page<WorkflowRunDB> pageWorkflowRun(@PathVariable String workflowId) {
        initContext();
        return ws.listWorkflowRun(WorkflowRunPage.builder().workflowId(workflowId).build());
    }

    private static DifyRunHistory transfer(WorkflowRunDB e) {
        return DifyRunHistory.builder()
                .id(e.getWorkflowRunId())
                .version(String.valueOf(e.getWorkflowVersion()))
                .status(e.getStatus())
                .created_by_account(Account.builder().id(String.valueOf(e.getCuid())).name(e.getCuName()).email("").build())
                .created_at(e.getCtime().toEpochSecond(ZoneOffset.UTC))
                .finished_at(e.getMtime().toEpochSecond(ZoneOffset.UTC)).build();
    }

    private static DifyRunHistoryDetails transfer(WorkflowRunDB wr, WorkflowDB wf) {
        WorkflowSchema workflowSchema = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
        return DifyRunHistoryDetails.builder()
                .id(wr.getWorkflowRunId())
                .version(wr.getWorkflowVersion() == 0 ? "draft" : String.valueOf(wr.getWorkflowVersion()))
                .status(wr.getStatus())
                .created_by_account(
                        Account.builder().id(String.valueOf(wr.getCuid())).name(wr.getCuName()).email("").build())
                .created_at(wr.getCtime().toEpochSecond(ZoneOffset.UTC))
                .finished_at(wr.getMtime().toEpochSecond(ZoneOffset.UTC))
                .graph(workflowSchema.getGraph())
                .inputs(JsonUtils.fromJson(wr.getInputs(), Map.class)).build();
    }

    @RequestMapping("/{workflowId}/workflow-runs")
    public Page<DifyRunHistory> pageWorkflowRuns(@PathVariable String workflowId,
            @RequestParam(value = "last_id", required = false) String lastId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        initContext();
        Assert.isTrue(limit > 0, "limit必须大于0");
        Assert.isTrue(limit < 100, "limit必须小于100");
        WorkflowRunPage page = WorkflowRunPage.builder().lastId(lastId).pageSize(limit).workflowId(workflowId).build();
        Page<WorkflowRunDB> workflowRunsDbPage = ws.listWorkflowRun(
                page);
        List<WorkflowRunDB> workflowRunsDb = workflowRunsDbPage.getData();
        AtomicInteger counter = new AtomicInteger(1);
        List<DifyRunHistory> list = workflowRunsDb.stream()
                .map(DifyController::transfer)
                .peek(result -> result.setSequence_number(counter.getAndIncrement()))
                .sorted(Comparator.comparing(DifyRunHistory::getFinished_at).reversed())
                .collect(Collectors.toList());
        Page<DifyRunHistory> result = new Page<>();
        result.setPage(page.getPage());
        result.pageSize(page.getPageSize());
        result.total(workflowRunsDbPage.getTotal());
        result.list(list);
        return result;
    }

    @GetMapping("/{workflowId}/workflow-runs/{workflowRunId}")
    public DifyRunHistoryDetails getWorkflowRun(@PathVariable String workflowId,
            @PathVariable String workflowRunId) {
        initContext();
        WorkflowRunDB wr = ws.getWorkflowRun(workflowRunId);
        WorkflowDB wf = ws.getWorkflow(workflowId, wr.getWorkflowVersion());
        return transfer(wr, wf);
    }

    @RequestMapping("/{workflowId}/workflow-runs/{workflowRunId}/node-executions")
    public DifyNodeExecution getWorkflowNodeRuns(@PathVariable String workflowId,
            @PathVariable String workflowRunId) {
        initContext();
        List<WorkflowNodeRunDB> nodeRuns = ws.getNodeRuns(workflowRunId);
        return DifyNodeExecution.builder().data(transfer(nodeRuns)).build();
    }

    private static List<DifyNodeExecution.DifyNodeRun> transfer(List<WorkflowNodeRunDB> nodeRunDBs) {
        AtomicInteger index = new AtomicInteger(1);
        AtomicReference<String> lastNodeId = new AtomicReference<>(null);
        List<DifyNodeExecution.DifyNodeRun> collect = nodeRunDBs.stream()
                .sorted(Comparator.comparing(WorkflowNodeRunDB::getCtime))
                .map(nodeRunDB -> {
                    DifyNodeExecution.DifyNodeRun nodeRun = createDifyNodeRun(nodeRunDB, index.getAndIncrement(), lastNodeId.get());
                    lastNodeId.set(nodeRunDB.getNodeId());
                    return nodeRun;
                })
                .collect(Collectors.toList());
        Collections.reverse(collect);
        return collect;
    }

    private static DifyNodeExecution.DifyNodeRun createDifyNodeRun(WorkflowNodeRunDB nodeRunDB, int index, String predecessorNodeId) {
        return DifyNodeExecution.DifyNodeRun.builder()
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
                .created_at(nodeRunDB.getCtime().toEpochSecond(ZoneOffset.UTC))
                .created_by_role("account")
                .created_by_account(Account.builder().id(String.valueOf(nodeRunDB.getCuid())).name(nodeRunDB.getCuName()).email("").build())
                .finished_at(nodeRunDB.getMtime().toEpochSecond(ZoneOffset.UTC))
                .build();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @SuperBuilder(toBuilder = true)
    public static class DifyNodeExecution {

        private List<DifyNodeRun> data;

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @SuperBuilder(toBuilder = true)
        public static class DifyNodeRun {
            private String id;
            private Integer index;
            private String predecessor_node_id;
            private String node_id;
            private String node_type;
            private String title;
            private Map inputs;
            private Map process_data;
            private Map outputs;
            private Object execution_metadata;
            @Builder.Default
            private Map extras = new HashMap();
            private String status;
            private Object error;
            private Double elapsed_time;
            private Long created_at;
            private String created_by_role;
            private Account created_by_account;
            private Long finished_at;
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @SuperBuilder(toBuilder = true)
    public static class DifyRunHistoryDetails extends DifyRunHistory {
        private WorkflowSchema.Graph graph;
        private Map inputs;
        private Object error;
        private Double elapsed_time;
        private Integer total_tokens;
        private Integer total_steps;
        @Builder.Default
        private String created_by_role = "account";
        private Object created_by_end_user;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @SuperBuilder(toBuilder = true)
    public static class DifyRunHistory {

        private String id;
        private Integer sequence_number;
        private String version;
        private Account created_by_account;
        private String status;
        private Long created_at;
        private Long finished_at;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class Account {
        private String id;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifyResponse {
        private Integer code;
        private String status;
        private String message;
    }

    private WorkflowSchema getDefaultWorkflowSchema() {
        WorkflowSchema.Graph graph = new WorkflowSchema.Graph();
        Map<String, Object> maps = Maps.newHashMap();
        maps.put("type", NodeType.START.name);
        maps.put("name", "开始");
        maps.put("title", "开始节点");
        maps.put("variables", Lists.newArrayList());
        maps.put("selected", true);

        graph.setNodes(Lists.newArrayList(WorkflowSchema.Node.builder()
                .id(System.currentTimeMillis() + "")
                .type(NodeType.START.name)
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

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class DifyWorkflowRun extends WorkflowOp {
        @Builder.Default
        Map inputs = new HashMap();

        @Builder.Default
        String responseMode = ResponseMode.streaming.name();

        String callbackUrl;

        @Builder.Default
        String triggerFrom = TriggerFrom.DEBUG.name();

        String query;
        List<File> files;
        @JsonAlias({ "conversation_id", "thread_id" })
        String threadId;
    }
}
