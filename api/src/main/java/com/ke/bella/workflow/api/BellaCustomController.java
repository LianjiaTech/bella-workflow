package com.ke.bella.workflow.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAggregateDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.service.WorkflowService;

/**
 * 提供给Bella的定制接口
 */
@RestController
@RequestMapping({ "/v1/custom/bella", "/console/api/custom/bella" })
public class BellaCustomController {

    @Autowired
    WorkflowService ws;

    @Autowired
    WorkflowController wc;

    @PostMapping("/app")
    public WorkflowDB createApp(@RequestBody DifyController.DifyApp app) {
        return wc.createApp0(app);
    }

    @GetMapping({ "/workflows", "" })
    public Page<WorkflowAggregateDB> pageWorkflowCustom(WorkflowOps.WorkflowPage op) {
        return ws.pageWorkflowAggregate(op);
    }

    @PostMapping("/delete")
    public void delete(@RequestBody WorkflowOps.WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        ws.deleteWorkflowAggregate(op.workflowId);
    }

}
