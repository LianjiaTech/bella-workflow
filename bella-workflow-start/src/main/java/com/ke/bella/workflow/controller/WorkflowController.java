package com.ke.bella.workflow.controller;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
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
    static class WorkflowOp {
        @NotEmpty
        String tenantId;

        @NotEmpty
        String workflowId;

        @NotEmpty
        String userId;

        String userName;
    }

    @RequestMapping(path = { "/info" }, method = { RequestMethod.GET, RequestMethod.POST })
    public WorkflowDB info(WorkflowOp op) {
        return null;
    }

    @Data
    static class WorkflowSync {
        @NotEmpty
        String tenantId;

        String workflowId;

        @NotEmpty
        String graph;
    }

    @PostMapping("/sync")
    public WorkflowDB sync(@Valid @RequestBody WorkflowSync op) {
        if(StringUtils.isEmpty(op.getWorkflowId())) {
            return ws.newWorkflow(op.tenantId, op.graph);
        }

        return ws.syncWorkflow(op.tenantId, op.workflowId, op.graph);
    }

    @PostMapping("/publish")
    public WorkflowDB publish(@Valid @RequestBody WorkflowOp op) {
        return null;
    }

    @Data
    @SuppressWarnings("rawtypes")
    static class WorkflowRun extends WorkflowOp {
        Map inputs;
    }

    @PostMapping("/run")
    public Object run(@Valid @RequestBody WorkflowRun op) {
        return null;
    }

    @Data
    @SuppressWarnings("rawtypes")
    static class WorkflowNodeRun extends WorkflowOp {
        @NotNull
        Map inputs;

        @NotEmpty
        String NodeId;
    }

    @PostMapping("/node/run")
    public Object runNode(@Valid @RequestBody WorkflowRun op) {
        return null;
    }

    @Data
    static class TenantCreate {
        String tenantName;
    }

    @PostMapping("/tenant/create")
    public TenantDB createTenant(@Valid @RequestBody TenantCreate op) {
        return null;
    }
}
