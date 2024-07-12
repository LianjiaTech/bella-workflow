package com.ke.bella.workflow.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAggregateDB;
import com.ke.bella.workflow.service.WorkflowService;

/**
 * 提供给Bella的定制接口
 */
@RestController
@RequestMapping("/console/api/custom/bella")
public class BellaCustomController {

    @Autowired
    WorkflowService ws;

    @GetMapping
    public Page<WorkflowAggregateDB> pageWorkflowCustom(WorkflowOps.WorkflowPage op) {
        return ws.pageWorkflowAggregate(op);
    }
}
