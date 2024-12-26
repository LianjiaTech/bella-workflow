package com.ke.bella.workflow.api;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.Operator;
import com.ke.bella.openapi.space.RoleWithSpace;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.IWorkflowCallback.File;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.WorkflowSchema.EnvVar;
import com.ke.bella.workflow.api.WorkflowOps.ResponseMode;
import com.ke.bella.workflow.api.WorkflowOps.TriggerFrom;
import com.ke.bella.workflow.api.WorkflowOps.TriggerType;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowAsApiPublish;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowOp;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowTrigger;
import com.ke.bella.workflow.api.callbacks.DifySingleNodeRunBlockingCallback;
import com.ke.bella.workflow.api.callbacks.DifyWorkflowRunStreamingCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunBlockingCallback;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAggregateDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAsApiDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.node.BaseNode;
import com.ke.bella.workflow.node.NodeType;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.service.WorkflowTriggerService;
import com.ke.bella.workflow.space.BellaSpaceService;
import com.ke.bella.workflow.utils.DifyUtils;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.OpenAiUtils;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageListSearchParameters;
import com.theokanning.openai.service.OpenAiService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/console/api/apps")
@Slf4j
public class DifyController {

    @Autowired
    WorkflowService ws;

    @Autowired
    WorkflowTriggerService ts;

    @Autowired
    BellaSpaceService ss;

    private void initContext(Operator op) {
        if(op != null && contextOperatorInvalid()) {
            BellaContext.setOperator(op);
        }
    }

    public static boolean contextOperatorInvalid() {
        return Objects.isNull(BellaContext.getOperatorIgnoreNull()) ||
                !StringUtils.hasText(BellaContext.getOperatorIgnoreNull().getTenantId()) ||
                !StringUtils.hasText(BellaContext.getOperatorIgnoreNull().getUserName()) ||
                Objects.isNull(BellaContext.getOperatorIgnoreNull().getUserId());
    }

    @GetMapping
    public Page<DifyApp> pageApps(@RequestParam int page, @RequestParam int limit, @RequestParam String name) {
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
                .cuid(wf.getCuid())
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
        WorkflowSchema schema = DifyUtils.getDefaultWorkflowSchema();
        WorkflowSync sync = WorkflowSync.builder()
                .title(app.getName())
                .desc(app.getDescription())
                .graph(JsonUtils.toJson(schema))
                .mode(app.getMode())
                .build();
        WorkflowDB wf = ws.newWorkflow(sync);
        app.setId(wf.getWorkflowId());
        app.setCuid(wf.getCuid());
        return app;
    }

    @GetMapping("/{workflowId}/workflows/draft")
    public WorkflowSchema getDraftInfo(@PathVariable String workflowId) {
        Assert.hasText(workflowId, "workflowId不能为空");
        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        if(Objects.isNull(wf)) {
            return DifyUtils.getDefaultWorkflowSchema();
        }

        return WorkflowSchema.fromWorkflowDB(wf);
    }

    @GetMapping(value = "/{workflowId}/export")
    public Object export(@PathVariable String workflowId, @RequestParam("include_secret") boolean inc) throws Exception {
        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        WorkflowSchema schema = WorkflowSchema.fromWorkflowDB(wf);
        List<EnvVar> vars = schema.getEnvironmentVariables();
        if(vars != null && !inc) {
            vars = vars.stream().filter(v -> !v.getType().equals("secret")).collect(Collectors.toList());
        }

        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("environment_variables", vars);
        obj.put("graph", schema.getGraph());

        return ImmutableMap.of("data", JsonUtils.toJson(obj));
    }

    @GetMapping("/{workflowId}/workflows")
    public Page<WorkflowDB> getWorkflows(@PathVariable String workflowId, @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
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
        String space_code;
        Long cuid;

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
        WorkflowAggregateDB wf = ws.getWorkflowAggregate(workflowId);
        return DifyApp.builder()
                .tenantId(wf.getTenantId())
                .id(workflowId)
                .name(wf.getTitle())
                .description(wf.getDesc())
                .mode(wf.getMode())
                .api_base_url(Configs.API_BASE)
                .cuid(wf.getCuid())
                .space_code(wf.getSpaceCode())
                .build();
    }

    @PutMapping("/{workflowId}")
    public DifyApp updateDifyApp(@PathVariable String workflowId, @RequestBody DifyApp app) {
        WorkflowSync op = WorkflowSync.builder()
                .title(app.getName())
                .desc(app.getDescription())
                .workflowId(workflowId)
                .build();
        WorkflowDB wf = ws.syncWorkflow(op);

        return DifyApp.builder()
                .tenantId(wf.getTenantId())
                .id(workflowId)
                .mode(wf.getMode())
                .name(wf.getTitle())
                .description(wf.getDesc())
                .mode(wf.getMode())
                .cuid(wf.getCuid())
                .api_base_url(Configs.API_BASE)
                .build();
    }

    @PostMapping("/{workflowId}/workflows/draft")
    public DifyResponse saveDraftInfo(@PathVariable String workflowId, @RequestBody WorkflowSchema schema, Operator op) {
        // 前端当页面退出等情况，使用navigator.sendBeacon的形式发送请求，此api不支持设置header，故此处通过请求参数实现。
        initContext(op);
        Assert.hasText(workflowId, "workflowId不能为空");
        if(schema == null) {
            schema = DifyUtils.getDefaultWorkflowSchema();
        }
        List<EnvVar> vars = schema.getEnvironmentVariables();

        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        WorkflowSync sync = WorkflowSync.builder()
                .graph(JsonUtils.toJson(schema))
                .envVars(JsonUtils.toJson(vars))
                .workflowId(workflowId)
                .build();
        if(Objects.isNull(wf)) {
            ws.newWorkflow(sync);
        } else {
            ws.syncWorkflow(sync);
        }
        wf = ws.getDraftWorkflow(workflowId);
        return DifyResponse.builder().code(200).message("保存成功").status("success").updatedAt(System.currentTimeMillis() / 1000).build();
    }

    @PostMapping("/{workflowId}/workflows/draft/import")
    public WorkflowSchema importDSL(@PathVariable String workflowId, @RequestBody WorkflowSchema dsl) {
        Assert.hasText(workflowId, "workflowId不能为空");
        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        WorkflowSync sync = WorkflowSync.builder()
                .graph(JsonUtils.toJson(dsl))
                .envVars(JsonUtils.toJson(dsl.getEnvironmentVariables()))
                .workflowId(workflowId)
                .build();
        if(Objects.isNull(wf)) {
            ws.newWorkflow(sync);
        } else {
            ws.syncWorkflow(sync);
        }
        return dsl;
    }

    @PostMapping("/{workflowId}/workflows/draft/nodes/{nodeId}/run")
    public Object nodeRun(@PathVariable String workflowId, @PathVariable String nodeId, @RequestBody WorkflowOps.WorkflowNodeRun op) {
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

        DifySingleNodeRunBlockingCallback callback = new DifySingleNodeRunBlockingCallback(ws, 300000L);
        ws.runNode(wr, nodeId, op.inputs, callback);
        return callback.getWorkflowNodeRunResult();
    }

    @GetMapping("/{workflowId}/workflows/publish")
    public Object getPublish(@PathVariable String workflowId) {
        Assert.hasText(workflowId, "workflowId不能为空");
        WorkflowDB wf = ws.getPublishedWorkflow(workflowId, null);
        if(Objects.isNull(wf)) {
            return DifyUtils.getDefaultWorkflowSchema();
        }
        return JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
    }

    @PostMapping("/{workflowId}/workflows/publish")
    public Object publish(@PathVariable String workflowId) {
        Assert.hasText(workflowId, "workflowId不能为空");
        try {
            WorkflowDB wf = ws.publish(workflowId);
            return DifyResponse.builder().code(200).message("发布成功").status("success")
                    .createdAt(wf.getCtime().atZone(ZoneId.systemDefault()).toEpochSecond()).build();
        } catch (Exception e) {
            return DifyResponse.builder().code(400).message(e.getMessage()).status("invalid_param").build();
        }
    }

    @PostMapping({ "/{workflowId}/workflows/draft/run" })
    public Object workflowRun(@PathVariable String workflowId, @RequestBody DifyWorkflowRun op) {
        return workflowRun0(workflowId, op);
    }

    @PostMapping({ "/{workflowId}/advanced-chat/workflows/draft/run" })
    public Object chatFlowRun(@PathVariable String workflowId, @RequestBody DifyWorkflowRun op) {
        return workflowRun0(workflowId, op);
    }

    private Object workflowRun0(String workflowId, DifyWorkflowRun op) {
        op.setWorkflowId(workflowId);
        ResponseMode mode = ResponseMode.valueOf(op.responseMode);
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
                .stateful(op.isStateful())
                .flashMode(op.flashMode)
                .build();

        WorkflowDB wf = ws.getDraftWorkflow(workflowId);
        Assert.notNull(wf, String.format("工作流[%s]当前无draft版本，无法调试", op2.workflowId));

        WorkflowRunDB wr = ws.newWorkflowRun(wf, op2);
        if(mode == ResponseMode.blocking) {
            WorkflowRunBlockingCallback callback = new WorkflowRunBlockingCallback(ws, 300000L);
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op2, callback));
            return callback.getWorkflowRunResult();
        } else {
            SseEmitter emitter = SseHelper.createSse(300000L, wr.getWorkflowRunId());
            TaskExecutor.submit(() -> {
                IWorkflowCallback callback = new DifyWorkflowRunStreamingCallback(emitter, ws);
                ws.runWorkflow(wr, op2, callback);
            });
            return emitter;
        }
    }

    @RequestMapping("/{workflowId}/workflow-app-logs")
    public Page<WorkflowRunDB> pageWorkflowRun(@PathVariable String workflowId) {
        return ws.listWorkflowRun(WorkflowRunPage.builder().workflowId(workflowId).build());
    }

    @RequestMapping("/{workflowId}/workflow-triggers")
    public Object listWorkflowTriggers(@PathVariable String workflowId, @RequestParam String triggerType) {
        List<WorkflowTrigger> triggers = ts.listWorkflowTriggers(workflowId, TriggerType.valueOf(triggerType));
        return ImmutableMap.of("data", triggers);
    }

    @PostMapping("/{workflowId}/trigger/create")
    public Object createWorkflowTrigger(@PathVariable String workflowId, @RequestBody WorkflowTrigger trigger) {
        WorkflowDB wd = ws.getPublishedWorkflow(workflowId, null);
        Assert.notNull(wd, "工作流需要先发布，才可以创建触发器");

        return ts.createWorkflowTrigger(workflowId, trigger);
    }

    @PostMapping("/{workflowId}/trigger/activate")
    public Object activateWorkflowTrigger(@PathVariable String workflowId, @RequestBody WorkflowTrigger trigger) {
        Assert.hasText(trigger.getTriggerId(), "triggerId不能为空");
        Assert.hasText(trigger.getTriggerType(), "triggerType不能为空");

        ts.activateWorkflowTrigger(trigger.getTriggerId(), trigger.getTriggerType());

        return trigger;
    }

    @PostMapping("/{workflowId}/trigger/deactivate")
    public Object deactivateWorkflowTrigger(@PathVariable String workflowId, @RequestBody WorkflowTrigger trigger) {
        Assert.hasText(trigger.getTriggerId(), "triggerId不能为空");
        Assert.hasText(trigger.getTriggerType(), "triggerType不能为空");

        ts.deactivateWorkflowTrigger(trigger.getTriggerId(), trigger.getTriggerType());
        return trigger;
    }

    @GetMapping("/{workflowId}/custom-apis")
    public Object listCustomApis(@PathVariable String workflowId) {
        List<WorkflowAsApiDB> triggers = ws.listCustomApis(workflowId);
        return ImmutableMap.of("data", triggers);
    }

    @PostMapping("/{workflowId}/customApi/create")
    public Object createCustomApi(@PathVariable String workflowId, @RequestBody WorkflowAsApiPublish op) {
        WorkflowDB wd = ws.getPublishedWorkflow(workflowId, null);
        Assert.notNull(wd, "工作流需要先发布，才可以创建自定义 API");

        op.setWorkflowId(workflowId);
        return ws.publishAsApi(op);
    }

    @GetMapping("/{workflowId}/workflow-versions")
    public Page<DifyWorkflowVersion> listWorkflowVersions(@PathVariable(value = "workflowId") String workflowId,
            @RequestParam(value = "last_id", defaultValue = "1") int lastId,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        Assert.isTrue(limit > 0, "limit必须大于0");
        Assert.isTrue(limit <= 100, "limit必须小于100");
        WorkflowPage page = WorkflowPage.builder().page(lastId).pageSize(limit).workflowId(workflowId).build();
        Page<WorkflowDB> workflowDbPage = ws.pagePublicWorkflows(page);
        List<DifyWorkflowVersion> list = workflowDbPage.getData().stream().map(DifyUtils::transfer).collect(Collectors.toList());
        Page<DifyWorkflowVersion> result = new Page<>();
        result.setPage(page.getPage());
        result.pageSize(page.getPageSize());
        result.total(workflowDbPage.getTotal());
        result.setData(list);
        return result;
    }

    @GetMapping("/{workflowId}/workflow-versions/default")
    public Object getDefaultWorkflowVersion(@PathVariable(value = "workflowId") String workflowId) {
        Assert.hasText(workflowId, "workflowId不能为空");
        return ws.getWorkflowAggregate(workflowId);
    }

    @PostMapping("/{workflowId}/workflow-versions/activate")
    public Object activateWorkflowVersions(@PathVariable(value = "workflowId") String workflowId,
            @RequestBody WorkflowOp op) {
        Assert.hasText(workflowId, "workflowId不能为空");
        ws.activateDefaultVersions(op);
        return ws.getWorkflowAggregate(workflowId);
    }

    @PostMapping("/{workflowId}/workflow-versions/deactivate")
    public Object deactivateWorkflowVersions(@PathVariable(value = "workflowId") String workflowId,
            @RequestBody WorkflowOp op) {
        ws.deactivateDefaultVersions(op);
        return ws.getWorkflowAggregate(workflowId);
    }

    @RequestMapping(path = { "/{workflowId}/workflow-runs", "/{workflowId}/advanced-chat/workflow-runs" })
    public Page<DifyRunHistory> pageWorkflowRuns(HttpServletRequest request, @PathVariable String workflowId,
            @RequestParam(value = "last_id", required = false) String lastId,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        Assert.isTrue(limit > 0, "limit必须大于0");
        Assert.isTrue(limit < 101, "limit必须小于100");
        WorkflowRunPage page = WorkflowRunPage.builder().lastId(lastId).pageSize(limit).workflowId(workflowId).build();
        Page<WorkflowRunDB> workflowRunsDbPage = ws.listWorkflowRun(page);
        List<WorkflowRunDB> workflowRunsDb = workflowRunsDbPage.getData();
        AtomicInteger counter = new AtomicInteger(1);
        List<WorkflowRunDB> workflows = null;
        // 如果thread_id之前已出现，则将此WorkflowRun从List中剔除

        // 获取请求路径，如果是advanced chat，则需要去重
        if(request.getRequestURI().contains("advanced-chat")) {
            workflows = new ArrayList<>();
            Set<String> threadIds = new HashSet<>();
            for (WorkflowRunDB workflowRunDB : workflowRunsDb) {
                if(!threadIds.contains(workflowRunDB.getThreadId())) {
                    threadIds.add(workflowRunDB.getThreadId());
                    workflows.add(workflowRunDB);
                }
            }
        } else {
            workflows = workflowRunsDb;
        }

        List<DifyRunHistory> list = workflows.stream()
                .map(DifyUtils::transfer)
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

    @RequestMapping("/{workflowId}/chat-messages")
    public Page<DifyChatFlowRun> pageChatFlowRuns(@PathVariable String workflowId,
            @RequestParam(value = "conversation_id", required = false) String threadId) {
        OpenAiService openAiService = OpenAiUtils.defaultOpenAiService(BellaContext.getApikey().getApikey());

        List<Message> messages = openAiService.listMessages(threadId, new MessageListSearchParameters()).getData();
        Collections.reverse(messages);
        List<List<Message>> groupedMessages = new ArrayList<>();
        List<Message> currentGroup = null;

        List<DifyChatFlowRun> chatFlowRuns = new ArrayList<>();

        try {
            for (Message message : messages) {
                if(message.getRole().equals("user")) {
                    currentGroup = new ArrayList<>();
                    groupedMessages.add(currentGroup);
                    currentGroup.add(message);
                } else {
                    if(CollectionUtils.isEmpty(currentGroup)) {
                        continue;
                    }
                    currentGroup.add(message);
                }
            }

            for (List<Message> groupedMessage : groupedMessages) {
                DifyChatFlowRun run = DifyChatFlowRun.builder()
                        .id(groupedMessage.get(0).getId())
                        .conversation_id(groupedMessage.get(0).getThreadId())
                        .query(groupedMessage.get(0).getContent().get(0).getText().getValue())
                        .answer(groupedMessage.subList(1, groupedMessage.size()).stream().map(e -> e.getContent().get(0).getText().getValue())
                                .collect(Collectors.joining()))
                        .created_at((long) groupedMessage.get(0).getCreatedAt())
                        .workflow_run_id(groupedMessage.get(0).getRunId())
                        .build();
                chatFlowRuns.add(run);
            }
        } catch (Exception e) {
            LOGGER.warn("pageChatFlowRuns error={}", Throwables.getStackTraceAsString(e));
            // fixme
        }

        Page<DifyChatFlowRun> result = new Page<>();
        result.setPage(1);
        result.pageSize(chatFlowRuns.size());
        result.total(chatFlowRuns.size());
        result.list(chatFlowRuns);
        return result;
    }

    @GetMapping("/{workflowId}/workflow-runs/{workflowRunId}")
    public DifyRunHistoryDetails getWorkflowRun(@PathVariable String workflowId,
            @PathVariable String workflowRunId) {
        WorkflowRunDB wr = ws.getWorkflowRun(workflowRunId);
        WorkflowDB wf = ws.getWorkflow(workflowId, wr.getWorkflowVersion());
        return DifyUtils.transfer(wr, wf);
    }

    @RequestMapping("/{workflowId}/workflow-runs/{workflowRunId}/node-executions")
    public DifyNodeExecution getWorkflowNodeRuns(@PathVariable String workflowId,
            @PathVariable String workflowRunId) {
        List<WorkflowNodeRunDB> nodeRuns = ws.getNodeRuns(workflowRunId);
        return DifyNodeExecution.builder().data(DifyUtils.transfer(nodeRuns)).build();
    }

    @GetMapping("/{workflowId}/workflows/default-workflow-block-configs/{blockType}")
    public Object defaultBlockConfigs(@PathVariable(value = "workflowId") String workflowId,
            @PathVariable(value = "blockType", required = false) String blockType,
            @RequestParam(value = "q", required = false) String query) {
        return BaseNode.defaultConfigs(NodeType.of(blockType), JsonUtils.fromJson(query, Map.class));
    }

    @GetMapping("/{workflowId}/workflows/default-workflow-block-configs")
    public Object defaultBlockConfigs(@PathVariable(value = "workflowId") String workflowId) {
        return BaseNode.defaultConfigs();
    }

    public RoleWithSpace getSpaceRole() {
        return ss.userSpaceRoles();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @SuperBuilder(toBuilder = true)
    public static class DifyRole {
        private String code;
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
        private String conversation_id;
        private String message_id;
        private String version;
        private Account created_by_account;
        private String status;
        private Long created_at;
        private Long finished_at;
        private Double elapsed_time;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @SuperBuilder(toBuilder = true)
    public static class DifyChatFlowRun {
        private String id;
        private String conversation_id;
        private String query;
        private String answer;
        private Long created_at;
        private String workflow_run_id;
        private String from_account_id;
        @Builder.Default
        private String status = "normal";
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
        @JsonProperty("created_at")
        private Long createdAt;
        @JsonProperty("updated_at")
        private Long updatedAt;
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

        @Builder.Default
        boolean stateful = true;

        @Builder.Default
        int flashMode = 0;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifyWorkflowVersion {
        private Long id;
        private String tenantId;
        private String workflowId;
        private String title;
        private String mode;
        private String desc;
        private Long version;
        private Long cuid;
        private String cuName;
        private Long ctime;
        private Long muid;
        private String muName;
        private Long mtime;
        private WorkflowSchema.Graph graph;
    }

}
