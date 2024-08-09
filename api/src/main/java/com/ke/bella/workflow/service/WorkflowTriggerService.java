package com.ke.bella.workflow.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.api.WorkflowOps.KafkaTriggerCreate;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.repo.WorkflowTriggerRepo;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowKafkaTriggerDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.trigger.WorkflowSchedulingStatus;
import com.ke.bella.workflow.utils.CronUtils;
import com.ke.bella.workflow.utils.JsonUtils;

@Component
public class WorkflowTriggerService {

    @Autowired
    WorkflowTriggerRepo repo;

    @Autowired
    WorkflowService ws;

    @Autowired
    WorkflowClient wfc;

    @SuppressWarnings("rawtypes")
    public WorkflowSchedulingDB createSchedulingTrigger(String workflowId, String cronExpression, Map inputs,
            LocalDateTime nextTriggerTime) {
        return repo.insertWorkflowScheduling(workflowId, cronExpression, nextTriggerTime, JsonUtils.toJson(inputs));
    }

    public void refreshTriggerNextTime(WorkflowSchedulingDB scheduling) {
        LocalDateTime nextExecution = CronUtils.nextExecution(scheduling.getCronExpression());
        if(Objects.isNull(nextExecution)) {
            finishedWorkflowScheduling(scheduling.getWorkflowSchedulingId());
        } else {
            updateWorkflowScheduling(scheduling.getWorkflowSchedulingId(), nextExecution);
        }
    }

    private void finishedWorkflowScheduling(String workflowSchedulingId) {
        WorkflowSchedulingDB scheduling = new WorkflowSchedulingDB();
        scheduling.setWorkflowSchedulingId(workflowSchedulingId);
        scheduling.setStatus(WorkflowSchedulingStatus.finished.name());
        repo.updateWorkflowScheduling(scheduling);
    }

    public void updateWorkflowScheduling(String workflowSchedulingId, LocalDateTime nextExecution) {
        WorkflowSchedulingDB scheduling = new WorkflowSchedulingDB();
        scheduling.setWorkflowSchedulingId(workflowSchedulingId);
        scheduling.setTriggerNextTime(nextExecution);
        repo.updateWorkflowScheduling(scheduling);
    }

    public Page<WorkflowSchedulingDB> pageWorkflowScheduling(WorkflowOps.WorkflowSchedulingPage op) {
        return repo.pageWorkflowScheduling(op);
    }

    @Transactional
    public WorkflowSchedulingDB stopWorkflowScheduling(WorkflowOps.WorkflowSchedulingOp op) {
        updateWorkflowSchedulingStatus(op.getTenantId(), op.getTriggerId(), WorkflowSchedulingStatus.stopped);
        return repo.selectWorkflowScheduling(op.getTenantId(), op.getTriggerId());
    }

    @Transactional
    public WorkflowSchedulingDB startWorkflowScheduling(WorkflowOps.WorkflowSchedulingOp op) {
        updateWorkflowSchedulingStatus(op.getTenantId(), op.getTriggerId(), WorkflowSchedulingStatus.running);
        return repo.selectWorkflowScheduling(op.getTenantId(), op.getTriggerId());
    }

    public void updateWorkflowSchedulingStatus(String tenantId, String triggerId, WorkflowSchedulingStatus status) {
        WorkflowSchedulingDB scheduling = new WorkflowSchedulingDB();
        scheduling.setTriggerId(triggerId);
        scheduling.setTenantId(tenantId);
        scheduling.setStatus(status.name());
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
        return repo.addKafkaTrigger(op);
    }

    public void deactiveKafkaTrigger(String triggerId) {
        repo.deactiveKafkaTrigger(triggerId);
    }

	public WorkflowKafkaTriggerDB queryKafkaTrigger(String triggerId) {
		return repo.queryKafkaTrigger(triggerId);
	}
}
