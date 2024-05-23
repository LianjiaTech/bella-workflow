package com.ke.bella.workflow.service;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowGraph;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowRunner;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.repo.WorkflowRepo;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.node.JsonUtils;

@Component
public class WorkflowService {

    @Resource
    WorkflowRepo repo;

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB newWorkflow(String graph) {
        return repo.addDraftWorkflow(null, graph);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncWorkflow(String workflowId, String graph) {
        WorkflowDB wf = repo.queryDraftWorkflow(workflowId);
        if (wf == null) {
            repo.addDraftWorkflow(workflowId, graph);
        } else {
            repo.updateDraftWorkflow(workflowId, graph);
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
        // 校验是否有过成功的调试记录
        Page<WorkflowRunDB> page = repo.pageWorkflowRun(workflowId, "succeeded");
        if(page.getList().isEmpty()) {
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
    public WorkflowRunDB newWorkflowRun(WorkflowDB wf, Map inputs, String callbackUrl) {
        return repo.addWorkflowRun(wf, JsonUtils.toJson(inputs), callbackUrl);
    }
}
