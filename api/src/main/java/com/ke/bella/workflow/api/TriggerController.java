package com.ke.bella.workflow.api;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.service.WorkflowSchedulingService;
import com.ke.bella.workflow.utils.CronUtils;

@RestController
@RequestMapping("/v1/workflow/trigger")
public class TriggerController {

    @Autowired
    WorkflowSchedulingService ws;

    @PostMapping("/scheduling/create")
    public WorkflowSchedulingDB createScheduling(@RequestBody WorkflowOps.WorkflowScheduling op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.cronExpression, "cronExpression不能为空");

        List<LocalDateTime> nextTimes = CronUtils.nextExecutions(op.getCronExpression(), 2);
        Assert.isTrue(!nextTimes.isEmpty(), "不存在晚于当前时间的下次执行时间，请检查cron表达式");
        if(nextTimes.size() == 2) {
            Assert.isTrue(ChronoUnit.SECONDS.between(nextTimes.get(0), nextTimes.get(1)) < 5 * 60,
                    "不允许两次时间间隔小于5分钟，请检查cron表达式");
        }

        WorkflowSchedulingDB wsDb = ws.createSchedulingTrigger(op.getWorkflowId(), op.getCronExpression(), op.getInputs(), nextTimes.get(0));
        return wsDb;
    }

    @PostMapping("/scheduling/stop")
    public WorkflowSchedulingDB stopScheduling(@RequestBody WorkflowOps.WorkflowSchedulingOp op) {
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getWorkflowSchedulingId(), "workflowSchedulingId不能为空");
        return ws.stopWorkflowScheduling(op);
    }

    @PostMapping("/scheduling/start")
    public WorkflowSchedulingDB startScheduling(@RequestBody WorkflowOps.WorkflowSchedulingOp op) {
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getWorkflowSchedulingId(), "workflowSchedulingId不能为空");
        return ws.startWorkflowScheduling(op);
    }

    @PostMapping("/scheduling/run")
    public BellaResponse<WorkflowRunDB> runScheduling(@RequestBody WorkflowOps.WorkflowSchedulingOp op) {
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getWorkflowSchedulingId(), "workflowSchedulingId不能为空");
        return BellaResponse.<WorkflowRunDB>builder().code(201).data(ws.runWorkflowScheduling(op)).build();
    }

    @PostMapping("/scheduling/page")
    public Page<WorkflowSchedulingDB> getScheduling(@RequestBody WorkflowOps.WorkflowSchedulingPage op) {
        Assert.notNull(op, "body不能为空");
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        return ws.pageWorkflowScheduling(op);
    }

    @PostMapping("/scheduling/workflow-runs")
    public Page<WorkflowRunDB> pageScheduling(@RequestBody WorkflowOps.WorkflowSchedulingRunPage op) {
        Assert.notNull(op, "body不能为空");
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getWorkflowSchedulingId(), "workflowSchedulingId不能为空");
        return ws.pageWorkflowRuns(op);
    }

    @PostMapping("/scheduling/callback/{workflow_scheduling_id}")
    public void callback(@PathVariable("workflow_scheduling_id") String workflowSchedulingId, @RequestBody Object obj) {
        // todo:
    }
}
