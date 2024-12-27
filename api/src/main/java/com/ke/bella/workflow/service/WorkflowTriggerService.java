package com.ke.bella.workflow.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.api.WorkflowOps.KafkaTriggerCreate;
import com.ke.bella.workflow.api.WorkflowOps.TriggerType;
import com.ke.bella.workflow.api.WorkflowOps.WebotTriggerCreate;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowScheduling;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowTrigger;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.repo.WorkflowTriggerRepo;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowKafkaTriggerDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowWebotTriggerDB;
import com.ke.bella.workflow.trigger.ExpressionHelper;
import com.ke.bella.workflow.trigger.WorkflowSchedulingStatus;
import com.ke.bella.workflow.utils.CronUtils;
import com.ke.bella.workflow.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class WorkflowTriggerService {

    @Autowired
    WorkflowTriggerRepo repo;

    @Autowired
    WorkflowService ws;

    @Autowired
    WorkflowClient wfc;

    public WorkflowSchedulingDB createSchedulingTrigger(WorkflowScheduling op) {
        WorkflowSchedulingDB workflowSchedulingDB = repo.insertWorkflowScheduling(op);
        return repo.selectWorkflowScheduling(workflowSchedulingDB.getTenantId(), workflowSchedulingDB.getTriggerId());
    }

    public void refreshTriggerNextTime(WorkflowSchedulingDB trigger) {
        try {
            LocalDateTime nextExecution = CronUtils.nextExecution(trigger.getCronExpression());
            if(Objects.isNull(nextExecution)) {
                finishedWorkflowScheduling(trigger.getTriggerId());
            } else {
                updateWorkflowScheduling(trigger.getTriggerId(), nextExecution);
            }
        } catch (Exception e) {
            updateWorkflowSchedulingStatus(trigger.getTenantId(), trigger.getTriggerId(), WorkflowSchedulingStatus.stopped);
            LOGGER.warn("failed to refresh scheduling trigger: " + trigger.getTriggerId(), e);
        }
    }

    private void finishedWorkflowScheduling(String triggerId) {
        WorkflowSchedulingDB scheduling = new WorkflowSchedulingDB();
        scheduling.setTriggerId(triggerId);
        scheduling.setRunningStatus(WorkflowSchedulingStatus.finished.name());
        repo.updateWorkflowScheduling(scheduling);
    }

    public void updateWorkflowScheduling(String workflowSchedulingId, LocalDateTime nextExecution) {
        WorkflowSchedulingDB scheduling = new WorkflowSchedulingDB();
        scheduling.setTriggerId(workflowSchedulingId);
        scheduling.setTriggerNextTime(nextExecution);
        repo.updateWorkflowScheduling(scheduling);
    }

    public Page<WorkflowSchedulingDB> pageWorkflowScheduling(WorkflowOps.WorkflowSchedulingPage op) {
        return repo.pageWorkflowScheduling(op);
    }

    @Transactional
    public WorkflowSchedulingDB stopWorkflowScheduling(WorkflowOps.WorkflowSchedulingStatusOp op) {
        deactivateWorkflowTrigger(op.getTriggerId(), op.getTriggerType());
        return repo.selectWorkflowScheduling(op.getTenantId(), op.getTriggerId());
    }

    @Transactional
    public WorkflowSchedulingDB startWorkflowScheduling(WorkflowOps.WorkflowSchedulingStatusOp op) {
        activateWorkflowTrigger(op.getTriggerId(), op.getTriggerType());
        return repo.selectWorkflowScheduling(op.getTenantId(), op.getTriggerId());
    }

    public void updateWorkflowSchedulingStatus(String tenantId, String triggerId, WorkflowSchedulingStatus status) {
        WorkflowSchedulingDB scheduling = new WorkflowSchedulingDB();
        scheduling.setTriggerId(triggerId);
        scheduling.setTenantId(tenantId);
        scheduling.setRunningStatus(status.name());
        repo.updateWorkflowScheduling(scheduling);
    }

    public List<WorkflowSchedulingDB> listPendingTask(LocalDateTime endTime, Integer batch) {
        return repo.listWorkflowScheduling(endTime,
                Sets.newHashSet(WorkflowSchedulingStatus.init.name(),
                        WorkflowSchedulingStatus.running.name()),
                batch);
    }

    public Page<WorkflowRunDB> pageWorkflowRuns(WorkflowOps.WorkflowSchedulingPage op) {
        WorkflowSchedulingDB wfs = Optional.ofNullable(repo.selectWorkflowScheduling(op.getTenantId(), op.getTriggerId()))
                .orElseThrow(() -> new IllegalArgumentException("workflowScheduling not found"));
        WorkflowOps.WorkflowRunPage runOp = WorkflowOps.WorkflowRunPage.builder().triggerId(wfs.getTriggerId())
                .workflowId(wfs.getWorkflowId())
                .lastId(op.getLastId()).pageSize(op.getPageSize()).build();
        return ws.listWorkflowRun(runOp);
    }

    public WorkflowRunDB runWorkflowScheduling(WorkflowOps.WorkflowSchedulingOp op) {
        WorkflowSchedulingDB wfsDb = Optional.ofNullable(repo.selectWorkflowScheduling(op.getTenantId(), op.getTriggerId()))
                .orElseThrow(() -> new IllegalArgumentException("workflowScheduling not found"));
        TenantDB tenant = ws.listTenants(Lists.newArrayList(wfsDb.getTenantId())).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("tenant not found"));
        return wfc.workflowRun(tenant, wfsDb);
    }

    public WorkflowKafkaTriggerDB createKafkaTrigger(KafkaTriggerCreate op) {
        ExpressionHelper.validate(op.getExpressionType(), op.getExpression());
        WorkflowKafkaTriggerDB wlkDb = repo.addKafkaTrigger(op);
        return repo.queryKafkaTrigger(wlkDb.getTriggerId());
    }

    public void activeKafkaTrigger(String triggerId) {
        repo.activeKafkaTrigger(triggerId);
    }

    public void deactiveKafkaTrigger(String triggerId) {
        repo.deactiveKafkaTrigger(triggerId);
    }

    public WorkflowKafkaTriggerDB queryKafkaTrigger(String triggerId) {
        return repo.queryKafkaTrigger(triggerId);
    }

    public WorkflowWebotTriggerDB createWebotTrigger(WebotTriggerCreate op) {
        if(StringUtils.hasText(op.getExpression())) {
            ExpressionHelper.validate(op.getExpressionType(), op.getExpression());
        }
        return repo.addWebotTrigger(op);
    }

    public void deactiveWebotTrigger(String triggerId) {
        repo.deactiveWebotTrigger(triggerId);
    }

    public WorkflowWebotTriggerDB queryWebotTrigger(String triggerId) {
        return repo.queryWebotTrigger(triggerId);
    }

    public List<WorkflowTrigger> listWorkflowTriggers(String workflowId, TriggerType type) {
        List<WorkflowTrigger> res = new ArrayList<>();
        if(type == TriggerType.KFKA) {
            List<WorkflowKafkaTriggerDB> triggers = repo.listKafkaTriggersWithWorkflow(workflowId);
            triggers.forEach(t -> {
                WorkflowTrigger tt = WorkflowTrigger.builder()
                        .triggerId(t.getTriggerId())
                        .triggerType(t.getTriggerType())
                        .name(t.getName())
                        .desc(t.getDesc())
                        .expression(t.getExpression())
                        .inputs(t.getInputs())
                        .status(t.getStatus().intValue() == 0 ? "active" : "inactive")
                        .build();
                res.add(tt);
            });

        } else if(type == TriggerType.SCHD) {
            List<WorkflowSchedulingDB> triggers = repo.listWorkflowSchedulingWithWorkflow(workflowId);
            triggers.forEach(t -> {
                WorkflowTrigger tt = WorkflowTrigger.builder()
                        .triggerId(t.getTriggerId())
                        .triggerType(t.getTriggerType())
                        .name(t.getName())
                        .desc(t.getDesc())
                        .inputs(t.getInputs())
                        .expression(t.getCronExpression())
                        .status(t.getStatus().intValue() == 0 ? "active" : "inactive")
                        .build();
                res.add(tt);
            });

        } else if(type == TriggerType.WBOT) {
            List<WorkflowWebotTriggerDB> triggers = repo.listWebotTriggersWithWorkflow(workflowId);
            triggers.forEach(t -> {
                WorkflowTrigger tt = WorkflowTrigger.builder()
                        .triggerId(t.getTriggerId())
                        .triggerType(t.getTriggerType())
                        .name(t.getName())
                        .desc(t.getDesc())
                        .inputs(t.getInputs())
                        .expression(t.getExpression())
                        .status(t.getStatus().intValue() == 0 ? "active" : "inactive")
                        .build();
                res.add(tt);
            });
        }
        return res;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowTrigger createWorkflowTrigger(String workflowId, WorkflowTrigger trigger) {
        TriggerType type = TriggerType.valueOf(trigger.getTriggerType());
        if(type == TriggerType.KFKA) {
            KafkaTriggerCreate op = KafkaTriggerCreate.builder()
                    .datasourceId(trigger.getDatasourceId())
                    .workflowId(workflowId)
                    .expression(trigger.getExpression())
                    .expressionType(trigger.getExpressionType())
                    .inputs(JsonUtils.fromJson(trigger.getInputs(), Map.class))
                    .inputkey(trigger.getInputKey())
                    .name(trigger.getName())
                    .desc(trigger.getDesc())
                    .build();
            WorkflowKafkaTriggerDB t = createKafkaTrigger(op);
            return WorkflowTrigger.builder()
                    .triggerId(t.getTriggerId())
                    .triggerType(t.getTriggerType())
                    .name(t.getName())
                    .desc(t.getDesc())
                    .expression(t.getExpression())
                    .status(t.getStatus().intValue() == 0 ? "active" : "inactive")
                    .build();

        } else if(type == TriggerType.SCHD) {
            WorkflowScheduling op = WorkflowScheduling.builder()
                    .workflowId(workflowId)
                    .workflowId(workflowId)
                    .cronExpression(trigger.getExpression())
                    .inputs(JsonUtils.fromJson(trigger.getInputs(), Map.class))
                    .name(trigger.getName())
                    .desc(trigger.getDesc())
                    .build();
            WorkflowSchedulingDB t = createSchedulingTrigger(op);
            return WorkflowTrigger.builder()
                    .triggerId(t.getTriggerId())
                    .triggerType(t.getTriggerType())
                    .name(t.getName())
                    .desc(t.getDesc())
                    .expression(t.getCronExpression())
                    .status(t.getStatus().intValue() == 0 ? "active" : "inactive")
                    .build();
        } else if(type == TriggerType.WBOT) {
            // TODO
        }
        return trigger;
    }

    public void activateWorkflowTrigger(String triggerId, String triggerType) {
        TriggerType type = TriggerType.valueOf(triggerType);
        if(type == TriggerType.KFKA) {
            repo.activeKafkaTrigger(triggerId);
        } else if(type == TriggerType.SCHD) {
            repo.activeWorkflowScheduling(triggerId);
            WorkflowSchedulingDB trigger = repo.selectWorkflowScheduling(triggerId);
            refreshTriggerNextTime(trigger);
        } else if(type == TriggerType.WBOT) {
            repo.activeWebotTrigger(triggerId);
        }
    }

    public void deactivateWorkflowTrigger(String triggerId, String triggerType) {
        TriggerType type = TriggerType.valueOf(triggerType);
        if(type == TriggerType.KFKA) {
            deactiveKafkaTrigger(triggerId);
        } else if(type == TriggerType.SCHD) {
            repo.deactiveWorkflowScheduling(triggerId);
        } else if(type == TriggerType.WBOT) {
            deactiveWebotTrigger(triggerId);
        }
    }
}
