package com.ke.bella.workflow.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.api.WorkflowOps.KafkaTriggerCreate;
import com.ke.bella.workflow.api.WorkflowOps.TriggerDeactivate;
import com.ke.bella.workflow.api.WorkflowOps.TriggerQuery;
import com.ke.bella.workflow.api.WorkflowOps.WebotTriggerCreate;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowKafkaTriggerDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowWebotTriggerDB;
import com.ke.bella.workflow.service.WorkflowTriggerService;
import com.ke.bella.workflow.trigger.WebotTriggerRunner;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/workflow/trigger")
public class TriggerController {

    @Autowired
    WorkflowTriggerService ws;

    @Autowired
    WebotTriggerRunner webot;

    @PostMapping("/scheduling/create")
    public WorkflowSchedulingDB createScheduling(@RequestBody WorkflowOps.WorkflowScheduling op) {
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.cronExpression, "cronExpression不能为空");

        WorkflowSchedulingDB wsDb = ws.createSchedulingTrigger(op);
        return wsDb;
    }

    @PostMapping("/scheduling/stop")
    public WorkflowSchedulingDB stopScheduling(@RequestBody WorkflowOps.WorkflowSchedulingStatusOp op) {
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getTriggerId(), "triggerId不能为空");
        Assert.hasText(op.getTriggerType(), "triggerType不能为空");
        return ws.stopWorkflowScheduling(op);
    }

    @PostMapping("/scheduling/start")
    public WorkflowSchedulingDB startScheduling(@RequestBody WorkflowOps.WorkflowSchedulingStatusOp op) {
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getTriggerId(), "triggerId不能为空");
        Assert.hasText(op.getTriggerType(), "triggerType不能为空");
        return ws.startWorkflowScheduling(op);
    }

    @PostMapping("/scheduling/run")
    public BellaResponse<WorkflowRunDB> runScheduling(@RequestBody WorkflowOps.WorkflowSchedulingOp op) {
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getTriggerId(), "triggerId不能为空");
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
        Assert.hasText(op.getTriggerId(), "triggerId不能为空");
        return ws.pageWorkflowRuns(op);
    }

    @PostMapping("/callback/{triggerId}")
    public void callback(@PathVariable("triggerId") String triggerId, @RequestBody Object obj) {
        // todo:
    }

    @PostMapping("/kafka/create")
    public WorkflowKafkaTriggerDB createKafkaTrigger(@RequestBody KafkaTriggerCreate op) {
        Assert.notNull(op, "body不能为空");
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getDatasourceId(), "datasourceId不能为空");
        Assert.hasText(op.getExpression(), "expression不能为空");
        Assert.hasText(op.getExpressionType(), "expression不能为空");
        Assert.hasText(op.getInputkey(), "inputKey不能为空");

        return ws.createKafkaTrigger(op);
    }

    @PostMapping("/kafka/deactive")
    public void deactiveKafkaTrigger(@RequestBody TriggerDeactivate op) {
        Assert.notNull(op, "body不能为空");
        Assert.hasText(op.getTriggerId(), "triggerId不能为空");

        ws.deactiveKafkaTrigger(op.getTriggerId());
    }

    @PostMapping("/kafka/info")
    public Object queryKafkaTrigger(@RequestBody TriggerQuery op) {
        Assert.notNull(op, "body不能为空");
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getTriggerId(), "triggerId不能为空");

        return ws.queryKafkaTrigger(op.getTriggerId());
    }

    @PostMapping("/webot/create")
    public WorkflowWebotTriggerDB createWebotTrigger(@RequestBody WebotTriggerCreate op) {
        Assert.notNull(op, "body不能为空");
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getRobotId(), "robotId不能为空");
        Assert.hasText(op.getInputkey(), "inputKey不能为空");
        Assert.isTrue(!StringUtils.hasText(op.getExpression()) || StringUtils.hasText(op.getExpressionType()), "expressionType不能为空");
        return ws.createWebotTrigger(op);
    }

    @PostMapping("/webot/deactive")
    public void deactiveWebotTrigger(@RequestBody TriggerDeactivate op) {
        Assert.notNull(op, "body不能为空");
        Assert.hasText(op.getTriggerId(), "triggerId不能为空");

        ws.deactiveWebotTrigger(op.getTriggerId());
    }

    @PostMapping("/webot/info")
    public Object queryWebotTrigger(@RequestBody TriggerQuery op) {
        Assert.notNull(op, "body不能为空");
        Assert.hasText(op.getTenantId(), "tenantId不能为空");
        Assert.hasText(op.getTriggerId(), "triggerId不能为空");

        return ws.queryWebotTrigger(op.getTriggerId());
    }

    @PostMapping("/webot/message/recv")
    public void recvWebotMessage(@RequestBody WechatCallBackBody body) {
        LOGGER.info("recv webot message: {}", JsonUtils.toJson(body));
        TaskExecutor.submit(() -> webot.recv(body));
    }
}
