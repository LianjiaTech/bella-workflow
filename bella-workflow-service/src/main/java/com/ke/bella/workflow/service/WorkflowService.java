package com.ke.bella.workflow.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowGraph;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunner;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.db.repo.WorkflowRepo;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;

@Component
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
        return repo.addDraftWorkflow(op);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncWorkflow(WorkflowSync op) {
        WorkflowDB wf = repo.queryDraftWorkflow(op.getWorkflowId());
        if(wf == null) {
            repo.addDraftWorkflow(op);
        } else {
            repo.updateDraftWorkflow(op);
        }
    }

    public WorkflowDB getDraftWorkflow(String workflowId) {
        return repo.queryDraftWorkflow(workflowId);
    }

    public WorkflowDB getPublishedWorkflow(String workflowId) {
        return repo.queryPublishedWorkflow(workflowId);
    }

    public WorkflowDB getWorkflow(String workflowId, Long version) {
        return repo.queryWorkflow(workflowId, version);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(String workflowId) {
        // 校验工作流配置是否合法
        WorkflowDB wf = getDraftWorkflow(workflowId);
        validateWorkflow(wf);

        // 校验是否有过成功的调试记录
        WorkflowRunDB wr = repo.queryDraftWorkflowRunSuccessed(wf);
        if(wr == null) {
            throw new IllegalArgumentException("工作流还未调试通过，请至少完整执行成功一次");
        }

        repo.publishWorkflow(workflowId);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantDB createTenant(String tenantName) {
        return repo.addTenant(tenantName);
    }

    @SuppressWarnings("rawtypes")
    public void runWorkflow(WorkflowRunDB wr, Map inputs, IWorkflowCallback callback) {
        // 校验工作流是否合法
        WorkflowDB wf = getWorkflow(wr.getWorkflowId(), wr.getWorkflowVersion());
        validateWorkflow(wf);

        // 构建执行上下文
        WorkflowSchema meta = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
        WorkflowGraph graph = new WorkflowGraph(meta);
        WorkflowContext context = WorkflowContext.builder()
                .tenantId(wr.getTenantId())
                .workflowId(wr.getWorkflowId())
                .runId(wr.getWorkflowRunId())
                .graph(graph)
                .state(new WorkflowRunState())
                .userInputs(inputs)
                .build();
        new WorkflowRunner().run(context, new WorkflowRunCallback(this, callback));
    }

    @SuppressWarnings("rawtypes")
    public void runNode(WorkflowRunDB wr, String nodeId, Map inputs, IWorkflowCallback callback) {
        // 校验工作流是否合法
        WorkflowDB wf = getWorkflow(wr.getWorkflowId(), wr.getWorkflowVersion());
        validateWorkflow(wf);

        // 构建执行上下文
        WorkflowSchema meta = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
        WorkflowGraph graph = new WorkflowGraph(meta);
        WorkflowContext context = WorkflowContext.builder()
                .tenantId(wr.getTenantId())
                .workflowId(wr.getWorkflowId())
                .runId(wr.getWorkflowRunId())
                .graph(graph)
                .state(new WorkflowRunState())
                .userInputs(inputs)
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

    @SuppressWarnings("rawtypes")
    public WorkflowRunDB newWorkflowRun(WorkflowDB wf, Map inputs, String callbackUrl, String responseMode) {
        final WorkflowRunDB wr = repo.addWorkflowRun(wf, JsonUtils.toJson(inputs), callbackUrl, responseMode);
        TaskExecutor.submit(() -> counter.increase(wr));
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

        repo.updateWorkflowRun(wr);
    }

    public void markWorkflowRunCallbacked(String workflowRunId) {
        WorkflowRunDB wr = new WorkflowRunDB();
        wr.setWorkflowRunId(workflowRunId);
        wr.setCallbackStatus(1);
        repo.updateWorkflowRun(wr);
    }

    public void createWorkflowNodeRun(WorkflowContext context, String nodeId, String status) {
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

        Node meta = context.getGraph().node(nodeId);
        wnr.setNodeType(meta.getType());
        wnr.setTitle(meta.getTitle());

        repo.addWorkflowRunNode(wnr);
    }

    public void updateWorkflowNodeRun(WorkflowContext context, String nodeId, String status) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setStatus(status);

        repo.updateWorkflowNodeRun(wnr);
    }

    public void updateWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setStatus(NodeRunResult.Status.succeeded.name());

        NodeRunResult nodeState = context.getState().getNodeState(nodeId);
        wnr.setInputs(JsonUtils.toJson(nodeState.getInputs()));
        wnr.setOutputs(JsonUtils.toJson(nodeState.getOutputs()));
        wnr.setProcessData(JsonUtils.toJson(nodeState.getProcessData()));
        wnr.setActivedTargetHandles(JsonUtils.toJson(nodeState.getActivatedSourceHandles()));
        wnr.setElapsedTime(nodeState.getElapsedTime());

        repo.updateWorkflowNodeRun(wnr);
    }

    public void updateWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(context.getTenantId());
        wnr.setWorkflowId(context.getWorkflowId());
        wnr.setWorkflowRunId(context.getRunId());
        wnr.setNodeId(nodeId);
        wnr.setStatus(NodeRunResult.Status.failed.name());
        wnr.setError(error);

        NodeRunResult nodeState = context.getState().getNodeState(nodeId);
        wnr.setInputs(JsonUtils.toJson(nodeState.getInputs()));
        wnr.setOutputs(JsonUtils.toJson(nodeState.getOutputs()));
        wnr.setProcessData(JsonUtils.toJson(nodeState.getProcessData()));
        wnr.setElapsedTime(nodeState.getElapsedTime());

        repo.updateWorkflowNodeRun(wnr);
    }

    public List<WorkflowRunDB> listWorkflowRun(String workflowId, LocalDateTime startTime) {
        return repo.listWorkflowRun(workflowId, startTime);
    }

    @SuppressWarnings("rawtypes")
    public void notifyWorkflowRun(String tenantId, String workflowId, String workflowRunId, String nodeId, Map inputs) {
        WorkflowNodeRunDB wnr = new WorkflowNodeRunDB();
        wnr.setTenantId(tenantId);
        wnr.setWorkflowId(workflowId);
        wnr.setWorkflowRunId(workflowRunId);
        wnr.setNodeId(nodeId);
        wnr.setStatus(NodeRunResult.Status.notified.name());
        wnr.setNotifyData(JsonUtils.toJson(inputs));

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

        if(ids.isEmpty()) {
            return false;
        }

        context.getState().putNotifyData(notifiedData);
        new WorkflowRunner().resume(context, new WorkflowRunCallback(this, callback), ids);
        return true;
    }

    public WorkflowRunDB getWorkflowRun(String workflowRunId) {
        return repo.queryWorkflowRun(workflowRunId);
    }
}
