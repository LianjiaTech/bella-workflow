package com.ke.bella.workflow.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ke.bella.workflow.BellaContext;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.api.callbacks.DifySingleNodeRunBlockingCallback;
import com.ke.bella.workflow.api.callbacks.DifyWorkflowRunStreamingCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunBlockingCallback;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.node.NodeType;
import com.ke.bella.workflow.service.WorkflowService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/console/api/apps")
public class DifyController {

    @Autowired
    WorkflowService ws;

    private void initContext() {
        BellaContext.setOperator(Operator.builder()
                .userId(userIdL)
                .tenantId("test")
                .userName("test")
                .build());
        BellaContext.setApiKey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8");
    }

    @GetMapping
    public Page<DifyApp> pageApps(@RequestParam int page, @RequestParam int limit, @RequestParam String name) {
        initContext();

        WorkflowPage op = WorkflowPage.builder().page(page).pageSize(limit).name(name).build();

        Page<WorkflowDB> wfs = ws.pageDraftWorkflow(op);

        List<DifyApp> apps = new ArrayList<>();
        wfs.getData().forEach(wf -> apps.add(DifyApp.builder()
                .id(wf.getWorkflowId())
                .name(wf.getTitle())
                .description(wf.getDesc())
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DifyApp {
        String id;
        String name;
        String description;
        @Builder.Default
        String mode = "workflow";
        @Builder.Default
        String icon = "\ud83e\udd16";
        @Builder.Default
        String icon_background = "#FFEAD5";
        boolean enable_site;
        boolean enable_api;
        Object model_config;
        @Builder.Default
        Site site = new Site();
        @Builder.Default
        String api_base_url = "https://example.com/v1";
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
                .id(workflowId)
                .name(wf.getTitle())
                .description(wf.getDesc())
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

        return app;
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
                .triggerFrom(WorkflowOps.TriggerFrom.DEBUG.name())
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
        WorkflowDB wf = ws.getPublishedWorkflow(workflowId);
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
            return DifyResponse.builder().code("invalid_param").message(e.getMessage()).status(400).build();
        }
        return DifyResponse.builder().code("success").message("发布成功").status(200).build();
    }

    @PostMapping("/{workflowId}/workflows/draft/run")
    public Object workflowRun(@PathVariable String workflowId, @RequestBody WorkflowOps.WorkflowRun op) {
        initContext();
        WorkflowOps.ResponseMode mode = WorkflowOps.ResponseMode.valueOf(op.responseMode);
        Assert.hasText(workflowId, "workflowId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");

        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        Assert.notNull(wf, String.format("工作流[%s]当前无draft版本，无法调试", op.workflowId));

        WorkflowRunDB wr = ws.newWorkflowRun(wf, op);
        if(mode == WorkflowOps.ResponseMode.blocking) {
            WorkflowRunBlockingCallback callback = new WorkflowRunBlockingCallback(ws, 300000L);
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op.inputs, callback));
            return callback.getWorkflowRunResult();
        } else {
            SseEmitter emitter = SseHelper.createSse(300000L, wr.getWorkflowRunId());
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op.inputs, new DifyWorkflowRunStreamingCallback(emitter)));
            return emitter;
        }
    }

    @RequestMapping("/{workflowId}/workflow-app-logs")
    public Page<WorkflowRunDB> pageWorkflowRun(@PathVariable String workflowId) {
        initContext();
        return ws.listWorkflowRun(WorkflowRunPage.builder().workflowId(workflowId).build());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifyResponse {
        private String code;
        private Integer status;
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

}
