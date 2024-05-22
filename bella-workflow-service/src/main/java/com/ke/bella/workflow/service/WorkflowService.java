package com.ke.bella.workflow.service;

import java.util.Map;

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
import com.ke.bella.workflow.node.Utils;

@Component
public class WorkflowService {

    WorkflowRepo repo;

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDB newWorkflow(String graph) {
        return repo.addWorkflow(graph);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncWorkflow(String workflowId, String graph) {
        repo.syncWorkflow(workflowId, graph);
    }

    public WorkflowDB getWorkflow(String workflowId) {
        return repo.queryWorkflow(workflowId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(String workflowId) {
        repo.publishWorkflow(workflowId);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantDB createTenant(String tenantName) {
        return repo.addTenant(tenantName);
    }

    @SuppressWarnings("rawtypes")
    public void runWorkflow(WorkflowRunDB wr, Map inputs, IWorkflowCallback callback) {
        // 校验工作流是否合法
        WorkflowDB wf = getWorkflow(wr.getWorkflowId());
        validateWorkflow(wf);

        // 构建执行上下文
        WorkflowSchema meta = Utils.fromJson(wf.getGraph(), WorkflowSchema.class);
        WorkflowGraph graph = new WorkflowGraph(meta);
        WorkflowContext context = WorkflowContext.builder()
                .graph(graph)
                .state(new WorkflowRunState())
                .userInputs(inputs)
                .build();
        new WorkflowRunner().run(context, new WorkflowRunCallback(this, callback));
    }

    @SuppressWarnings("rawtypes")
    public Object runNode(String workflowId, String nodeId, Map inputs, String responseMode) {
        // TODO Auto-generated method stub
        return null;
    }

    public void validateWorkflow(WorkflowDB wf) {
        // 校验工作流配置是否合法
        String graphJson = wf.getGraph();
        try {
            WorkflowSchema meta = Utils.fromJson(graphJson, WorkflowSchema.class);
            WorkflowGraph graph = new WorkflowGraph(meta);
            graph.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("工作流配置不合法: " + e.getMessage());
        }

        // 校验是否有过成功的调试记录
        Page<WorkflowRunDB> page = repo.pageWorkflowRun(wf.getWorkflowId(), "succeeded");
        if(page.getList().isEmpty()) {
            throw new IllegalArgumentException("工作流还未调试通过，请至少完整执行成功一次");
        }
    }

    public WorkflowRunDB newWorkflowRun(String workflowId, Map inputs, String responseMode, String callbackUrl) {
        // TODO Auto-generated method stub
        return null;
    }
}
