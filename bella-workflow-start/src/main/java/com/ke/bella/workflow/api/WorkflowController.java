package com.ke.bella.workflow.api;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.service.WorkflowService;

import lombok.Data;

@RestController
@RequestMapping("/v1/workflow")
public class WorkflowController {

    WorkflowService ws;

    @Data
    static class WorkflowOp extends Operator {
        String workflowId;
    }

    @RequestMapping(path = { "/info" }, method = { RequestMethod.POST })
    public WorkflowDB info(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.getWorkflow(op.getTenantId(), op.workflowId);
    }

    @Data
    static class WorkflowSync extends Operator {
        String workflowId;

        @NotEmpty
        String graph;
    }

    @PostMapping("/sync")
    public WorkflowDB sync(@RequestBody WorkflowSync op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.graph, "graph不能为空");

        if(StringUtils.isEmpty(op.getWorkflowId())) {
            return ws.newWorkflow(op.graph);
        }

        return ws.syncWorkflow(op.workflowId, op.graph);
    }

    @PostMapping("/publish")
    public WorkflowDB publish(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.publish(op.getTenantId(), op.getWorkflowId());
    }

    @Data
    @SuppressWarnings("rawtypes")
    static class WorkflowRun extends WorkflowOp {
        Map inputs;
    }

    @PostMapping("/run")
    public Object run(@RequestBody WorkflowRun op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");

        return null;
    }

    @Data
    @SuppressWarnings("rawtypes")
    static class WorkflowNodeRun extends WorkflowOp {
        Map inputs;
        String nodeId;
    }

    @PostMapping("/node/run")
    public Object runNode(@RequestBody WorkflowNodeRun op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.nodeId, "nodeId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");

        return null;
    }

    @Data
    static class TenantCreate extends Operator {
        String tenantName;
    }

    @PostMapping("/tenant/create")
    public TenantDB createTenant(@RequestBody TenantCreate op) {
        Assert.hasText(op.tenantName, "tenantName不能为空");

        return null;
    }
}
