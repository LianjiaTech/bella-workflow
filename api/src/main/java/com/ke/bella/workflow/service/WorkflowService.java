package com.ke.bella.workflow.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.google.common.cache.Cache;
import com.ke.bella.job.queue.worker.Task;
import com.ke.bella.openapi.apikey.ApikeyInfo;
import com.ke.bella.workflow.api.callbacks.WorkflowBatchRunCallback;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.s3.model.S3Object;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.protocol.files.File;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.RedisMesh;
import com.ke.bella.workflow.RedisMesh.Event;
import com.ke.bella.workflow.RedisMesh.MessageListener;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowGraph;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;
import com.ke.bella.workflow.WorkflowRunner;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.WorkflowSchema.EnvVar;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.WorkflowTemplate;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.api.WorkflowOps.ResponseMode;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowAsApiPublish;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunCancel;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.api.callbacks.WorkflowRunNotifyCallback;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.repo.WorkflowRepo;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAggregateDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAsApiDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowTemplateDB;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.OpenAiUtils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap;

import static com.ke.bella.openapi.BellaContext.BELLA_TRACE_HEADER;

@Component
@Slf4j
public class WorkflowService {

    @Resource
    WorkflowRepo repo;

    @Resource
    WorkflowRunCountUpdator counter;

    @Autowired
    AmazonS3Service s3;

    @Autowired
    RedisMesh mesh;

    @Autowired
    WorkflowRunLogService ls;

    WorkflowRunManager manager;

    public static final String EVENT_INTERRUPT = "interruptWorkflowRun";
    public static final String EVENT_NOTIFY = "notifyWorkflowRun";

    public Page<WorkflowTemplate> pageWorkflowTemplates(WorkflowPage op) {
        List<WorkflowTemplateDB> workflowTemplates = repo.listWorkflowTemplateDB(op);
        Set<String> tags = op.getTags();

        List<WorkflowTemplate> datas = null;
        if(!CollectionUtils.isEmpty(tags)) {
            datas = workflowTemplates.stream().map(WorkflowTemplate::from)
                    .filter(e -> Optional.ofNullable(e.getTags()).map(ts -> ts.containsAll(tags)).orElse(false)).collect(Collectors.toList());
        } else {
            datas = workflowTemplates.stream().map(WorkflowTemplate::from).collect(Collectors.toList());
        }

        int totalSize = datas.size();

        int fromIndex = (op.getPage() - 1) * op.getPageSize();
        int toIndex = Math.min(fromIndex + op.getPageSize(), totalSize);

        List<WorkflowTemplate> resultData = datas.subList(fromIndex, toIndex);

        int currentPage = op.getPage();
        int currentPageSize = resultData.size();

        return Page.<WorkflowTemplate>from(currentPage, currentPageSize).list(resultData).total(totalSize);
    }

    public WorkflowTemplate getWorkflowTemplate(WorkflowSync op) {
        WorkflowTemplateDB workflowTemplate = repo.queryWorkflowTemplate(op);
        return WorkflowTemplate.from(workflowTemplate);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB newWorkflowFromTemplate(WorkflowSync op) {
        repo.increaseTemplateCopies(op);

        WorkflowTemplateDB workflowTemplate = repo.queryWorkflowTemplate(op);
        WorkflowDB wf = repo.queryWorkflow(workflowTemplate.getWorkflowId(), workflowTemplate.getVersion());

        WorkflowSync sync = WorkflowSync.builder()
                .graph(wf.getGraph())
                .title(wf.getTitle())
                .mode(wf.getMode())
                .desc(wf.getDesc())
                .build();

        return newWorkflow(sync);
    }

    public WorkflowTemplate publishAsTemplate(WorkflowOps.WorkflowOp op) {
        WorkflowDB db = getPublishedWorkflow(op.getWorkflowId(), op.getVersion());
        Assert.notNull(db, "workflow not found");
        WorkflowTemplateDB templateDB = repo.addWorkflowTemplate(db, op);
        return WorkflowTemplate.from(templateDB);
    }

    @AllArgsConstructor
    public static class WaitingWorkflowRun {
        WorkflowContext context;
        IWorkflowCallback callback;
        Map<String, Object> bellaContext;
    }

    @PostConstruct
    public void init() {

        this.manager = new WorkflowRunManager(mesh);

        // update counter every 5s.
        TaskExecutor.scheduleAtFixedRate(() -> counter.flush(), 5);

        // try sharding every 60s.
        TaskExecutor.scheduleAtFixedRate(() -> counter.trySharding(), 60);

        mesh.registerListener(EVENT_INTERRUPT, new MessageListener() {
            @Override
            public void onMessage(Event e) {
                try {
                    BellaContext.replace(e.getContext());
                    interruptWorkflowRun(e.getPayload());
                } finally {
                    BellaContext.clearAll();
                }
            }
        });

        mesh.registerListener(EVENT_NOTIFY, new MessageListener() {
            @Override
            public void onMessage(Event e) {
                try {
                    BellaContext.replace(e.getContext());
                    String runId = e.getPayload();
                    tryResumeWorkflowRun(runId);
                } finally {
                    BellaContext.clearAll();
                }
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB newWorkflow(WorkflowSync op) {
        WorkflowDB workflowDB = repo.addWorkflow(op);
        repo.addWorkflowAggregate(workflowDB);
        return workflowDB;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB newWorkflowPublished(WorkflowDB wf) {
        // 添加一个published以及agg
        WorkflowSync sync = WorkflowSync.builder()
                .graph(wf.getGraph())
                .title(wf.getTitle())
                .mode(wf.getMode())
                .desc(wf.getDesc())
                .version(wf.getVersion())
                .build();
        WorkflowDB published = newWorkflow(sync);

        // 添加一份draft，保证画布可打开
        WorkflowSync draftSync = WorkflowSync.builder()
                .workflowId(published.getWorkflowId())
                .graph(published.getGraph())
                .title(published.getTitle())
                .mode(published.getMode())
                .desc(published.getDesc())
                .build();
        repo.addWorkflow(draftSync);

        return published;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB syncWorkflow(WorkflowSync op) {
        WorkflowDB wf = repo.queryDraftWorkflow(op.getWorkflowId());
        if(wf == null) {
            WorkflowDB workflowDb = repo.addWorkflow(op);
            repo.addWorkflowAggregate(workflowDb);
        } else if(!StringUtils.equals(wf.getEnvVars(), op.getEnvVars())) {
            repo.updateDraftWorkflow(op);
            repo.updateWorkflowAggregate(op);
        } else if(!StringUtils.equals(wf.getGraph(), op.getGraph())) {
            WorkflowSchema old = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
            WorkflowSchema opg = Objects.isNull(op.getGraph()) ? null : JsonUtils.fromJson(op.getGraph(), WorkflowSchema.class);
            if(!old.equals(opg)) {
                repo.updateDraftWorkflow(op);
                repo.updateWorkflowAggregate(op);
            }
        }
        return repo.queryDraftWorkflow(op.getWorkflowId());
    }

    public WorkflowDB getDraftWorkflow(String workflowId) {
        return repo.queryDraftWorkflow(workflowId);
    }

    public Page<WorkflowDB> pageDraftWorkflow(WorkflowPage op) {
        return repo.pageDraftWorkflow(op);
    }

    public WorkflowDB getPublishedWorkflow(String workflowId, Long version) {
        if(version == null) {
            WorkflowAggregateDB wfg = repo.queryWorkflowAggregate(workflowId);
            if(wfg == null) {
                return null;
            }
            version = wfg.getDefaultPublishVersion() > 0 ? wfg.getDefaultPublishVersion() : null;
        }
        return repo.queryPublishedWorkflow(workflowId, version);
    }

    public WorkflowDB getWorkflow(String workflowId, Long version) {
        return repo.queryWorkflow(workflowId, version);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB publish(String workflowId, String releaseDescription) {
        // 校验工作流配置是否合法
        WorkflowDB wf = getDraftWorkflow(workflowId);
        validateWorkflow(wf);

        // 校验是否有过成功的调试记录
        WorkflowRunDB wr = repo.queryDraftWorkflowRunSuccessed(wf);
        if(wr == null) {
            throw new IllegalArgumentException("工作流还未调试通过，请至少完整执行成功一次");
        }

        long version = repo.publishWorkflow(workflowId, releaseDescription);
        repo.publishWorkflowAggregate(workflowId, version);

        LOGGER.info("{} workflow published, version: {}", workflowId, version);
        return repo.queryWorkflow(workflowId, version);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantDB createTenant(String tenantName, String parentTenantId, String openapiKey) {
        return repo.addTenant(tenantName, parentTenantId, openapiKey);
    }

    public TenantDB getTenant(String tenantId) {
        return repo.getTenant(tenantId);
    }

    public String getTenantApiKey(String tenantId) {
        TenantDB db = repo.getTenant(tenantId);
        if(db != null) {
            return db.getOpenapiKey();
        }
        return "";
    }

    public List<TenantDB> listTenants(List<String> tenantIds) {
        return repo.listTenants(tenantIds);
    }

    public void runWorkflow(WorkflowRunDB wr, WorkflowRun op, IWorkflowCallback callback) {
        // 校验工作流是否合法
        WorkflowDB wf = getWorkflow(wr.getWorkflowId(), wr.getWorkflowVersion());

        // 获取文件
        List<File> files = new ArrayList<>();
        if(!CollectionUtils.isEmpty(op.getFileIds())) {
            files = OpenAiUtils.defaultOpenApiClient().listFiles(BellaContext.getApikey().getApikey(), op.getFileIds());
        }

        // 构建执行上下文
        WorkflowSchema meta = WorkflowSchema.fromWorkflowDB(wf);
        WorkflowGraph graph = new WorkflowGraph(meta);
        WorkflowRunState state = new WorkflowRunState();
        state.putVariable("sys", "query", op.getQuery());
        state.putVariable("sys", "files", files);
        state.putVariable("sys", "message_id", IDGenerator.newMessageId());
        state.putVariable("sys", "thread_id", op.getThreadId());
        state.putVariable("sys", "metadata", op.getMetadata());
        state.putVariable("sys", "tenant_id", wr.getTenantId());
        state.putVariable("sys", "workflow_id", wr.getWorkflowId());
        state.putVariable("sys", "run_id", wr.getWorkflowRunId());
        List<EnvVar> ennVars = meta.getEnvironmentVariables();
        if(ennVars != null) {
            ennVars.forEach(v -> state.putVariable("env", v.getName(), v.getValue()));
        }

        WorkflowContext context = WorkflowContext.builder()
                .tenantId(wr.getTenantId())
                .workflowId(wr.getWorkflowId())
                .runId(wr.getWorkflowRunId())
                .graph(graph)
                .state(state)
                .userInputs(op.getInputs())
                .triggerFrom(wr.getTriggerFrom())
                .ctime(wr.getCtime())
                .flashMode(wr.getFlashMode())
                .workflowMode(wf.getMode())
                .stateful(op.isStateful())
                .build();

        try {
            manager.beforeRun(context);
            new WorkflowRunner().run(context, new WorkflowRunCallback(this, callback));
        } finally {
            manager.afterRun(context);
        }
    }

    public void runWorkflow(Task task, Cache<String, WorkflowDB> workflowCache) {
        try {
            WorkflowOps.WorkflowRun payload = task.getPayload(WorkflowOps.WorkflowRun.class);
            payload.setTriggerFrom(WorkflowOps.TriggerFrom.BATCH.name());
            Map<String, Object> inputs = payload.getInputs();
            String apiKey = MapUtils.getString(inputs, "apiKey");
            String traceId = MapUtils.getString(inputs, "traceId",
                    BellaContext.generateTraceId("workflow-batch-run"));
            BellaContext.setOperator(payload);
            BellaContext.setApikey(ApikeyInfo.builder().apikey(apiKey).build());
            BellaContext.getHeaders().put(BELLA_TRACE_HEADER, traceId);

            TaskExecutor.submit(() -> {
                try {
                    String workflowId = payload.getWorkflowId();
					WorkflowDB workflowDB = workflowCache.get(workflowId,
						() -> getPublishedWorkflow(workflowId, null));
                    runWorkflow(newWorkflowRun(workflowDB, payload), payload, new WorkflowBatchRunCallback(task));
                } catch (Exception e) {
                    task.markFailed(e.getMessage());
                }
            }, e -> task.markRetryLater());
        } finally {
            BellaContext.clearAll();
        }
    }

    @SuppressWarnings("rawtypes")
    public void runNode(WorkflowRunDB wr, String nodeId, Map inputs, IWorkflowCallback callback) {
        // 校验工作流是否合法
        WorkflowDB wf = getWorkflow(wr.getWorkflowId(), wr.getWorkflowVersion());

        // 构建执行上下文
        WorkflowSchema meta = WorkflowSchema.fromWorkflowDB(wf);

        // 判断节点是否存在及构造上下文
        Node node = meta.getGraph().getNodes().stream().filter(n -> StringUtils.equals(n.getId(), nodeId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("节点不存在: " + nodeId));
        WorkflowGraph graph = new WorkflowGraph(meta, node.getParentId());
        WorkflowRunState state = new WorkflowRunState();
        WorkflowContext context = WorkflowContext.builder()
                .tenantId(wr.getTenantId())
                .workflowId(wr.getWorkflowId())
                .runId(wr.getWorkflowRunId())
                .ctime(wr.getCtime())
                .graph(graph)
                .state(state)
                .userInputs(inputs)
                .triggerFrom(wr.getTriggerFrom())
                .build();
        state.putVariable("sys", "tenant_id", wr.getTenantId());
        state.putVariable("sys", "workflow_id", wr.getWorkflowId());
        state.putVariable("sys", "run_id", wr.getWorkflowRunId());
        List<EnvVar> ennVars = meta.getEnvironmentVariables();
        if(ennVars != null) {
            ennVars.forEach(v -> state.putVariable("env", v.getName(), v.getValue()));
        }

        try {
            manager.beforeRun(context);
            new WorkflowRunner().runNode(context, new WorkflowRunCallback(this, callback), nodeId);
        } finally {
            manager.afterRun(context);
        }
    }

    public void validateWorkflow(WorkflowDB wf) {
        // 校验工作流配置是否合法
        String graphJson = wf.getGraph();
        try {
            WorkflowSchema meta = JsonUtils.fromJson(graphJson, WorkflowSchema.class);
            WorkflowGraph graph = new WorkflowGraph(meta);
            graph.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("工作流配置不合法: " + e.getMessage());
        }
    }

    public WorkflowRunDB newWorkflowRun(WorkflowDB wf, WorkflowRun op) {
        final WorkflowRunDB wr = repo.addWorkflowRun(wf, op);
        if(wr.getId() != null) {
            TaskExecutor.submit(() -> counter.increase(wr));
        }

        LOGGER.info("{} {} created new workflow run.", wf.getWorkflowId(), wr.getWorkflowRunId());
        return wr;
    }

    public void updateWorkflowRunStatus(WorkflowContext context, String status) {
        updateWorkflowRun(context, status, "");
    }

    public void markWorkflowRunStarted(WorkflowContext context) {
        updateWorkflowRunStatus(context, WorkflowRunStatus.running.name());
    }

    @SuppressWarnings("rawtypes")
    public void markWorkflowRunSuccessed(WorkflowContext context, Map outputs) {
        WorkflowRunDB wr = new WorkflowRunDB();
        wr.setTenantId(context.getTenantId());
        wr.setWorkflowId(context.getWorkflowId());
        wr.setWorkflowRunId(context.getRunId());
        wr.setStatus(WorkflowRunStatus.succeeded.name());
        wr.setElapsedTime(context.elapsedTime(LocalDateTime.now()));
        if(outputs != null && context.getFlashMode() < 3) {
            wr.setOutputs(JsonUtils.toJson(outputs));
        } else {
            wr.setOutputs("");
        }

        repo.updateWorkflowRunResult(wr);
        mesh.removeTask(context.getRunId());
    }

    public void updateWorkflowRun(WorkflowContext context, String status, String error) {
        WorkflowRunDB wr = new WorkflowRunDB();
        wr.setTenantId(context.getTenantId());
        wr.setWorkflowId(context.getWorkflowId());
        wr.setWorkflowRunId(context.getRunId());
        wr.setStatus(status);
        wr.setError(error);
        wr.setElapsedTime(context.elapsedTime(LocalDateTime.now()));
        wr.setThreadId(context.getThreadId());

        repo.updateWorkflowRunResult(wr);
    }

    public void markWorkflowRunCallbacked(String workflowRunId) {
        WorkflowRunDB wr = new WorkflowRunDB();
        wr.setWorkflowRunId(workflowRunId);
        wr.setCallbackStatus(1);
        repo.updateWorkflowRunCallbackStatus(wr);
    }

    public void createWorkflowNodeRun(WorkflowContext context, String nodeId, String nodeRunId, String status) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setStatus(status);
        wnr.setInputs("");
        wnr.setOutputs("");
        wnr.setError("");
        wnr.setProcessData("");
        wnr.setNotifyData("");
        wnr.setNodeRunId(nodeRunId);

        Node meta = context.getGraph().node(nodeId);
        wnr.setNodeType(meta.getNodeType());
        wnr.setTitle(meta.getTitle());

        repo.addWorkflowRunNode(wnr);
    }

    public void updateWorkflowNodeRun(WorkflowContext context, String nodeId, String nodeRunId, String status) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setNodeRunId(nodeRunId);
        wnr.setStatus(status);

        repo.updateWorkflowNodeRun(wnr);
    }

    public void updateWorkflowNodeRunWaited(WorkflowContext context, String nodeId, String nodeRunId) {
        // step1: sync data
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setNodeRunId(nodeRunId);

        NodeRunResult nodeState = context.getState().getNodeState(nodeId);
        wnr.setInputs(JsonUtils.toJson(nodeState.getInputs()));
        wnr.setProcessData(JsonUtils.toJson(nodeState.getProcessData()));
        wnr.setElapsedTime(nodeState.getElapsedTime());

        repo.updateWorkflowNodeRun(wnr);

        // step2: sync status if status match
        repo.updateWorkflowNodeRunStatusAsWaiting(wnr);
    }

    public void updateWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setNodeRunId(nodeRunId);
        wnr.setStatus(NodeRunResult.Status.succeeded.name());

        NodeRunResult nodeState = context.getState().getNodeState(nodeId);
        wnr.setInputs(JsonUtils.toJson(nodeState.getInputs()));
        wnr.setOutputs(JsonUtils.toJson(nodeState.getOutputs()));
        wnr.setProcessData(JsonUtils.toJson(nodeState.getProcessData()));
        wnr.setActivedTargetHandles(JsonUtils.toJson(nodeState.getActivatedSourceHandles()));
        wnr.setElapsedTime(nodeState.getElapsedTime());

        repo.updateWorkflowNodeRun(wnr);
    }

    public void updateWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setNodeRunId(nodeRunId);
        wnr.setStatus(NodeRunResult.Status.failed.name());
        wnr.setError(error);

        NodeRunResult nodeState = context.getState().getNodeState(nodeId);
        wnr.setInputs(JsonUtils.toJson(nodeState.getInputs()));
        wnr.setOutputs(JsonUtils.toJson(nodeState.getOutputs()));
        wnr.setProcessData(JsonUtils.toJson(nodeState.getProcessData()));
        wnr.setElapsedTime(nodeState.getElapsedTime());

        repo.updateWorkflowNodeRun(wnr);
    }

    public void updateWorkflowNodeRunException(WorkflowContext context, String nodeId, String nodeRunId) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setNodeRunId(nodeRunId);
        wnr.setStatus(NodeRunResult.Status.exception.name());

        NodeRunResult nodeState = context.getState().getNodeState(nodeId);
        wnr.setInputs(JsonUtils.toJson(nodeState.getInputs()));
        wnr.setOutputs(JsonUtils.toJson(nodeState.getOutputs()));
        wnr.setProcessData(JsonUtils.toJson(nodeState.getProcessData()));
        wnr.setElapsedTime(nodeState.getElapsedTime());
        wnr.setError(Optional.ofNullable(nodeState.getError()).map(Throwable::getMessage).orElse(""));

        repo.updateWorkflowNodeRun(wnr);
    }

    public Page<WorkflowRunDB> listWorkflowRun(WorkflowRunPage op) {
        return repo.listWorkflowRun(op);
    }

    @SuppressWarnings("rawtypes")
    public void notifyWorkflowRun(WorkflowRunDB wr, String nodeId, String nodeRunId, Map inputs) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(wr.getTenantId());
        wnr.setWorkflowId(wr.getWorkflowId());
        wnr.setWorkflowRunId(wr.getWorkflowRunId());
        wnr.setNodeId(nodeId);
        wnr.setStatus(NodeRunResult.Status.notified.name());
        wnr.setNotifyData(JsonUtils.toJson(inputs));
        wnr.setNodeRunId(nodeRunId);

        if(wr.getFlashMode().intValue() == 0) {
            repo.updateWorkflowNodeRun(wnr);
        } else {
            wnr.setInputs("");
            wnr.setOutputs("");
            wnr.setError("");
            wnr.setProcessData("");
            wnr.setNodeType("");
            wnr.setTitle("");
            repo.addWorkflowRunNode(wnr);
        }
    }

    public void cancelWorkflowRun(WorkflowRunCancel op) {
        cancelWorkflowRunStatus(op.getRunId());
        if(manager.isRunning(op.getRunId())) {
            interruptWorkflowRun(op.getRunId());
        } else {
            String target = mesh.getInstanceId(op.getRunId());
            Event e = Event.builder()
                    .context(JsonUtils.toJson(BellaContext.snapshot()))
                    .name(EVENT_INTERRUPT)
                    .payload(op.getRunId())
                    .build();
            if(StringUtils.isNotEmpty(target)) {
                mesh.sendPrivateMessage(target, e);
            } else {
                mesh.sendBroadcastMessage(e);
            }
        }
    }

    private void cancelWorkflowRunStatus(String runId) {
        WorkflowRunDB run = new WorkflowRunDB();
        run.setWorkflowRunId(runId);
        run.setStatus(WorkflowRunState.WorkflowRunStatus.stopped.name());
        run.setError("canceld");
        repo.updateWorkflowRunResult(run);
    }

    @SuppressWarnings("rawtypes")
    public boolean tryResumeWorkflow(WorkflowContext context, IWorkflowCallback callback) {
        Set<String> nodeids = context.getState().waitingNodeIds();
        List<WorkflowNodeRunDB> wrns = repo.queryWorkflowNodeRuns(context.getRunId(), nodeids);

        Map<String, String> ids = new HashMap<>();
        Map<String, Map> notifiedData = new HashMap<>();
        wrns.forEach(r -> {
            if(r.getStatus().equals(NodeRunResult.Status.notified.name())) {
                ids.put(r.getNodeId(), r.getNodeRunId());
                notifiedData.put(r.getNodeId(), JsonUtils.fromJson(r.getNotifyData(), Map.class));
            }
        });

        // 简化同步操作，等所有节点都回来再继续执行
        if(ids.isEmpty() || ids.size() != nodeids.size()) {
            return false;
        }

        context.getState().putNotifyData(notifiedData);
        try {
            manager.beforeRun(context);
            new WorkflowRunner().resume(context, new WorkflowRunCallback(this, callback), ids);
            return true;
        } finally {
            manager.afterRun(context);
        }
    }

    public WorkflowRunDB getWorkflowRun(String workflowRunId) {
        return repo.queryWorkflowRun(workflowRunId);
    }

    @SuppressWarnings("rawtypes")
    public void tryResumeWorkflow(String workflowRunId, IWorkflowCallback callback) {
        WorkflowRunDB wr = repo.queryWorkflowRun(workflowRunId);
        if(WorkflowRunStatus.valueOf(wr.getStatus()) != WorkflowRunStatus.suspended) {
            return;
        }

        // 构建执行上下文
        WorkflowDB wf = getWorkflow(wr.getWorkflowId(), wr.getWorkflowVersion());
        WorkflowSchema meta = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
        WorkflowGraph graph = new WorkflowGraph(meta);
        WorkflowRunState state = resumeWorkflowRunState(wr.getWorkflowRunId());
        WorkflowContext context = WorkflowContext.builder()
                .tenantId(wr.getTenantId())
                .workflowId(wr.getWorkflowId())
                .runId(wr.getWorkflowRunId())
                .graph(graph)
                .state(state)
                .userInputs(new HashMap())
                .triggerFrom(wr.getTriggerFrom())
                .ctime(wr.getCtime())
                .flashMode(wr.getFlashMode())
                .workflowMode(wf.getMode())
                .stateful(wr.getStateful().intValue() == 1)
                .build();

        tryResumeWorkflow(context, callback);
    }

    @SuppressWarnings("unchecked")
    public WorkflowRunState getWorkflowRunState(WorkflowRunDB wr) {
        List<WorkflowNodeRunDB> wrs = repo.queryWorkflowNodeRuns(wr.getWorkflowRunId());
        WorkflowRunState state = new WorkflowRunState();
        wrs.forEach(wnr -> state.putNodeState(wnr.getNodeId(), NodeRunResult.builder()
                .inputs(JsonUtils.fromJson(wnr.getInputs(), Map.class))
                .outputs(JsonUtils.fromJson(wnr.getOutputs(), Map.class))
                .processData(JsonUtils.fromJson(wnr.getProcessData(), Map.class))
                .status(NodeRunResult.Status.valueOf(wnr.getStatus()))
                .activatedSourceHandles(JsonUtils.fromJson(wnr.getActivedTargetHandles(), List.class))
                .build(), true));

        state.putVariable("sys", "query", wr.getQuery());
        state.putVariable("sys", "files", JsonUtils.fromJson(wr.getFiles(), List.class));
        state.putVariable("sys", "metadata", JsonUtils.fromJson(wr.getMetadata(), Map.class));
        state.putVariable("sys", "tenant_id", wr.getTenantId());
        state.putVariable("sys", "workflow_id", wr.getWorkflowId());
        state.putVariable("sys", "run_id", wr.getWorkflowRunId());
        return state;
    }

    public Page<WorkflowDB> pageWorkflows(WorkflowPage op) {
        return repo.pageWorkflows(op);
    }

    public Page<WorkflowDB> pagePublicWorkflows(WorkflowPage op) {
        return repo.pagePublishedWorkflows(op);
    }

    public void activateDefaultVersions(WorkflowOps.WorkflowOp op) {
        WorkflowDB wf = getPublishedWorkflow(op.getWorkflowId(), op.getVersion());
        repo.updateDefaultVersion(wf, wf.getVersion());
    }

    public void deactivateDefaultVersions(WorkflowOps.WorkflowOp op) {
        WorkflowDB wf = getDraftWorkflow(op.getWorkflowId());
        repo.updateDefaultVersion(wf, -1L);
    }

    public WorkflowAggregateDB getWorkflowAggregate(String workflowId) {
        return repo.queryWorkflowAggregate(workflowId);
    }

    public Page<WorkflowAggregateDB> pageWorkflowAggregate(WorkflowPage op) {
        return repo.pageWorkflowAggregate(op);
    }

    public List<WorkflowNodeRunDB> getNodeRuns(String workflowRunId) {
        return repo.queryWorkflowNodeRuns(workflowRunId);
    }

    public void deleteWorkflowAggregate(String workflowId) {
        repo.updateWorkflowAggregate(WorkflowSync.builder().workflowId(workflowId).status(-1).build());
    }

    public WorkflowAsApiDB getCustomApi(String host, String path) {
        String hash = HttpUtils.sha256(host + path);
        return repo.queryCustomApi(hash);
    }

    public List<WorkflowAsApiDB> listCustomApis(String workflowId) {
        return repo.listCustomApis(workflowId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowAsApiDB publishAsApi(WorkflowAsApiPublish op) {
        WorkflowDB wf = null;
        if(op.getVersion() == null) {
            wf = getPublishedWorkflow(op.getWorkflowId(), null);
        } else {
            wf = getWorkflow(op.getWorkflowId(), op.getVersion());
        }
        return repo.addCustomApi(wf, op);
    }

    public void dumpWorkflowRunState(WorkflowContext context) {
        try {
            LOGGER.info("{} {} dump workflow run state to s3", context.getWorkflowId(), context.getRunId());
            String json = JsonUtils.toJson(context.getState());
            s3.putObject(context.getRunId(), json);
        } catch (Exception e) {
            throw new IllegalStateException("faild to dump workflow run state to s3.", e);
        }
    }

    public WorkflowRunState resumeWorkflowRunState(String runId) {
        try {
            LOGGER.info("{} resume workflow run state from s3", runId);
            S3Object obj = s3.getObject(runId);
            return JsonUtils.fromJson(obj.getObjectContent(), WorkflowRunState.class);
        } catch (Exception e) {
            throw new IllegalStateException("faild to resume workflow run state from s3.", e);
        }
    }

    public void waitWorkflowRunNotify(WorkflowContext context, IWorkflowCallback callback, long timeout) {
        manager.waitWorkflowRunNotify(context, callback, timeout);
    }

    public void notifyWorkflowRun(WorkflowRunDB wr) {
        String runId = wr.getWorkflowRunId();
        String instanceId = mesh.getInstanceId(runId);
        if(StringUtils.isNotEmpty(instanceId) || manager.isRunning(runId)) {
            TaskExecutor.schedule(() -> notifyWorkflowRun(wr), 2000);
            return;
        }

        if(ResponseMode.callback.name().equals(wr.getResponseMode())) {
            try {
                BellaContext.replace(wr.getContext());
                WorkflowRunNotifyCallback callback = new WorkflowRunNotifyCallback(this, wr.getCallbackUrl());
                tryResumeWorkflow(wr.getWorkflowRunId(), callback);
            } finally {
                BellaContext.clearAll();
            }
        } else {
            WaitingWorkflowRun wcc = manager.requireWaitingWorkflowRun(runId);
            if(wcc != null) {
                try {
                    BellaContext.replace(wcc.bellaContext);
                    tryResumeWorkflowRun(runId);
                } finally {
                    BellaContext.clearAll();
                }
            } else {
                Event e = Event.builder()
                        .context(JsonUtils.toJson(BellaContext.snapshot()))
                        .name(EVENT_NOTIFY)
                        .payload(runId)
                        .build();
                if(StringUtils.isNotEmpty(instanceId)) {
                    mesh.sendPrivateMessage(instanceId, e);
                } else {
                    mesh.sendBroadcastMessage(e);
                }
            }
        }
    }

    private void interruptWorkflowRun(String runId) {
        WorkflowContext context = manager.getWorkflowContext(runId);
        if(context != null) {
            context.setInterrupted(true);
        }
    }

    private void tryResumeWorkflowRun(String runId) {
        WaitingWorkflowRun wcc = manager.requireWaitingWorkflowRun(runId);
        if(wcc != null) {
            try {
                BellaContext.replace(wcc.bellaContext);
                tryResumeWorkflow(wcc.context, wcc.callback);
            } finally {
                BellaContext.clearAll();
            }
        }
    }

    public static class WorkflowRunManager {
        final RedisMesh mesh;
        final Map<String, WorkflowContext> runnningWorkflowRuns = new ConcurrentHashMap<>();
        final ExpiringMap<String, WaitingWorkflowRun> waitingWorkflowRuns = ExpiringMap.builder().variableExpiration().maxSize(10240)
                .expiration(10, TimeUnit.MINUTES).asyncExpirationListener(new ExpirationListener<String, WaitingWorkflowRun>() {
                    @Override
                    public void expired(String key, WaitingWorkflowRun value) {
                        value.callback.onWorkflowRunFailed(value.context, "wait workflow resume timeout.", new TimeoutException());
                    }

                }).build();

        public WorkflowRunManager(RedisMesh mesh) {
            this.mesh = mesh;
        }

        public synchronized void beforeRun(WorkflowContext context) {
            if(runnningWorkflowRuns.containsKey(context.getRunId())) {
                throw new IllegalStateException("存在正在运行的run实例");
            }
            waitingWorkflowRuns.remove(context.getRunId());
            mesh.addTask(context.getRunId(), (int) context.getTimeout());
            runnningWorkflowRuns.put(context.getRunId(), context);
        }

        public synchronized void afterRun(WorkflowContext context) {
            runnningWorkflowRuns.remove(context.getRunId());
            mesh.removeTask(context.getRunId());
        }

        public void waitWorkflowRunNotify(WorkflowContext context, IWorkflowCallback callback, long timeout) {
            waitingWorkflowRuns.put(context.getRunId(),
                    new WaitingWorkflowRun(context, callback, BellaContext.snapshot()),
                    timeout, TimeUnit.SECONDS);
        }

        public WaitingWorkflowRun requireWaitingWorkflowRun(String runId) {
            return waitingWorkflowRuns.get(runId);
        }

        public boolean isRunning(String runId) {
            return runnningWorkflowRuns.containsKey(runId);
        }

        public synchronized WorkflowContext getWorkflowContext(String runId) {
            WorkflowContext context = runnningWorkflowRuns.get(runId);
            if(context == null) {
                WaitingWorkflowRun t = waitingWorkflowRuns.get(runId);
                if(t != null) {
                    return t.context;
                }
            }
            return null;
        }
    }

}
