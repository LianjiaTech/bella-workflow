package com.ke.bella.workflow.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowGraph;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;
import com.ke.bella.workflow.WorkflowRunner;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowAsApiPublish;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.repo.WorkflowRepo;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAggregateDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAsApiDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowService {

    @Resource
    WorkflowRepo repo;

    @Resource
    WorkflowRunCountUpdator counter;

    @PostConstruct
    public void init() {
        // update counter every 5s.
        TaskExecutor.scheduleAtFixedRate(() -> counter.flush(), 5);

        // try sharding every 60s.
        TaskExecutor.scheduleAtFixedRate(() -> counter.trySharding(), 60);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB newWorkflow(WorkflowSync op) {
        WorkflowDB workflowDb = repo.addDraftWorkflow(op);
        repo.addWorkflowAggregate(workflowDb);
        return workflowDb;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB syncWorkflow(WorkflowSync op) {
        WorkflowDB wf = repo.queryDraftWorkflow(op.getWorkflowId());
        if(wf == null) {
            WorkflowDB workflowDb = repo.addDraftWorkflow(op);
            repo.addWorkflowAggregate(workflowDb);
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
        return repo.queryPublishedWorkflow(workflowId, version);
    }

    public WorkflowDB getWorkflow(String workflowId, Long version) {
        return repo.queryWorkflow(workflowId, version);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB publish(String workflowId) {
        // 校验工作流配置是否合法
        WorkflowDB wf = getDraftWorkflow(workflowId);
        validateWorkflow(wf);

        // 校验是否有过成功的调试记录
        WorkflowRunDB wr = repo.queryDraftWorkflowRunSuccessed(wf);
        if(wr == null) {
            throw new IllegalArgumentException("工作流还未调试通过，请至少完整执行成功一次");
        }

        long version = repo.publishWorkflow(workflowId);
        repo.publishWorkflowAggregate(workflowId, version);

        LOGGER.info("{} workflow published, version: {}", workflowId, version);
        return repo.queryWorkflow(workflowId, version);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantDB createTenant(String tenantName, String parentTenantId) {
        return repo.addTenant(tenantName, parentTenantId);
    }

    public List<TenantDB> listTenants(List<String> tenantIds) {
        return repo.listTenants(tenantIds);
    }

    public void runWorkflow(WorkflowRunDB wr, WorkflowRun op, IWorkflowCallback callback) {
        // 校验工作流是否合法
        WorkflowDB wf = getWorkflow(wr.getWorkflowId(), wr.getWorkflowVersion());

        // 构建执行上下文
        WorkflowSchema meta = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
        WorkflowGraph graph = new WorkflowGraph(meta);
        WorkflowRunState state = new WorkflowRunState();
        state.putVariable("sys", "query", op.getQuery());
        state.putVariable("sys", "files", op.getFiles());
        state.putVariable("sys", "message_id", IDGenerator.newMessageId());
        state.putVariable("sys", "thread_id", op.getThreadId());
        state.putVariable("sys", "metadata", op.getMetadata());
        state.putVariable("sys", "tenant_id", wr.getTenantId());
        state.putVariable("sys", "workflow_id", wr.getWorkflowId());
        state.putVariable("sys", "run_id", wr.getWorkflowRunId());

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
        new WorkflowRunner().run(context, new WorkflowRunCallback(this, callback));
    }

    @SuppressWarnings("rawtypes")
    public void runNode(WorkflowRunDB wr, String nodeId, Map inputs, IWorkflowCallback callback) {
        // 校验工作流是否合法
        WorkflowDB wf = getWorkflow(wr.getWorkflowId(), wr.getWorkflowVersion());

        // 构建执行上下文
        WorkflowSchema meta = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);

        // 判断节点是否存在及构造上下文
        Node node = meta.getGraph().getNodes().stream().filter(n -> StringUtils.equals(n.getId(), nodeId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("节点不存在: " + nodeId));
        WorkflowGraph graph = new WorkflowGraph(meta, node.getParentId());

        WorkflowContext context = WorkflowContext.builder()
                .tenantId(wr.getTenantId())
                .workflowId(wr.getWorkflowId())
                .runId(wr.getWorkflowRunId())
                .graph(graph)
                .state(new WorkflowRunState())
                .userInputs(inputs)
                .triggerFrom(wr.getTriggerFrom())
                .build();
        new WorkflowRunner().runNode(context, new WorkflowRunCallback(this, callback), nodeId);
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
        TaskExecutor.submit(() -> counter.increase(wr));

        LOGGER.info("{} {} created new workflow run.", wf.getWorkflowId(), wr.getWorkflowRunId());
        return wr;
    }

    public void updateWorkflowRun(WorkflowContext context, String status) {
        updateWorkflowRun(context, status, "");
    }

    @SuppressWarnings("rawtypes")
    public void updateWorkflowRun(WorkflowContext context, String status, Map outputs) {
        WorkflowRunDB wr = new WorkflowRunDB();
        wr.setTenantId(context.getTenantId());
        wr.setWorkflowId(context.getWorkflowId());
        wr.setWorkflowRunId(context.getRunId());
        wr.setStatus(status);
        wr.setElapsedTime(context.elapsedTime(LocalDateTime.now()));
        if(outputs != null) {
            wr.setOutputs(JsonUtils.toJson(outputs));
        }

        repo.updateWorkflowRun(wr);
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

        repo.updateWorkflowRun(wr);
    }

    public void markWorkflowRunCallbacked(String workflowRunId) {
        WorkflowRunDB wr = new WorkflowRunDB();
        wr.setWorkflowRunId(workflowRunId);
        wr.setCallbackStatus(1);
        repo.updateWorkflowRun(wr);
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
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setNodeRunId(nodeRunId);
        wnr.setStatus(NodeRunResult.Status.waiting.name());

        NodeRunResult nodeState = context.getState().getNodeState(nodeId);
        wnr.setInputs(JsonUtils.toJson(nodeState.getInputs()));
        wnr.setProcessData(JsonUtils.toJson(nodeState.getProcessData()));
        wnr.setElapsedTime(nodeState.getElapsedTime());

        repo.updateWorkflowNodeRun(wnr);
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

        repo.updateWorkflowNodeRun(wnr);
    }

    @SuppressWarnings("rawtypes")
    public boolean tryResumeWorkflow(WorkflowContext context, IWorkflowCallback callback) {
        Set<String> nodeids = context.getState().waitingNodeIds();
        List<WorkflowNodeRunDB> wrns = repo.queryWorkflowNodeRuns(context.getRunId(), nodeids);

        List<String> ids = new ArrayList<>();
        Map<String, Map> notifiedData = new HashMap<>();
        wrns.forEach(r -> {
            if(r.getStatus().equals(NodeRunResult.Status.notified.name())) {
                ids.add(r.getNodeId());
                notifiedData.put(r.getNodeId(), JsonUtils.fromJson(r.getNotifyData(), Map.class));
            }
        });

        // 简化同步操作，等所有节点都回来再继续执行
        if(!ids.isEmpty() && ids.size() != nodeids.size()) {
            return false;
        }

        context.getState().putNotifyData(notifiedData);
        new WorkflowRunner().resume(context, new WorkflowRunCallback(this, callback), ids);
        return true;
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
        WorkflowContext context = WorkflowContext.builder()
                .tenantId(wr.getTenantId())
                .workflowId(wr.getWorkflowId())
                .runId(wr.getWorkflowRunId())
                .graph(graph)
                .state(getWorkflowRunState(wr))
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
                .build()));

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
}
