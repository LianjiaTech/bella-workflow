package com.ke.bella.workflow.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.WorkflowSchedulingStatus;
import com.ke.bella.workflow.db.repo.WorkflowSchedulingRepo;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.utils.CronUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowSchedulingService {

    @Autowired
    WorkflowSchedulingRepo repo;

    @Autowired
    WorkflowClient workflowClient;

    public WorkflowSchedulingDB createWorkflowScheduling(String workflowId, String cronExpression, Map inputs,
            LocalDateTime nextTriggerTime) {
        return repo.insertWorkflowScheduling(workflowId, cronExpression, nextTriggerTime, JsonUtils.toJson(inputs));
    }

    public void triggerScheduling(WorkflowSchedulingDB scheduling) {
        // invoke http api to run, todo: handle error response
        workflowClient.workflowRun(scheduling);
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

    public List<WorkflowSchedulingDB> listPendingTask(LocalDateTime endTime, Integer batch) {
        return repo.listWorkflowScheduling(endTime,
                Sets.newHashSet(WorkflowSchedulingStatus.init.name(),
                        WorkflowSchedulingStatus.running.name()),
                batch);
    }
}
