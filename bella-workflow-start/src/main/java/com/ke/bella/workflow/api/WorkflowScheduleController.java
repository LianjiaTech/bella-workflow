package com.ke.bella.workflow.api;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.service.WorkflowSchedulingService;
import com.ke.bella.workflow.utils.CronUtils;

@RestController
@RequestMapping("/v1/workflow/trigger")
public class WorkflowScheduleController {

    @Autowired
    WorkflowSchedulingService ws;

    @PostMapping("/scheduling")
    public BellaResponse createScheduling(@RequestBody WorkflowOps.WorkflowScheduling op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.cronExpression, "cronExpression不能为空");
        List<LocalDateTime> nextTimes = CronUtils.nextExecutions(op.getCronExpression(), 2);
        Assert.isTrue(nextTimes.size() > 0, "不存在晚于当前时间:" + ZonedDateTime.now().toLocalDateTime().toString() + "的下次执行时间，请检查cron表达式");
        if(nextTimes.size() == 2) {
            Assert.isTrue(ChronoUnit.SECONDS.between(nextTimes.get(0), nextTimes.get(1)) < 5,
                    "不允许两次时间间隔小于5s，请检查cron表达式");
        }
        WorkflowSchedulingDB wsDb = ws.createWorkflowScheduling(op.getWorkflowId(), op.getCronExpression(), op.getInputs(), nextTimes.get(0));
        return BellaResponse.builder().code(200)
                .data(wsDb).build();
    }

    @PostMapping("/scheduling/callback/{workflow_scheduling_id}")
    public void callback(@PathVariable("workflow_scheduling_id") String workflowSchedulingId, @RequestBody Object obj) {
        // todo:
    }
}
