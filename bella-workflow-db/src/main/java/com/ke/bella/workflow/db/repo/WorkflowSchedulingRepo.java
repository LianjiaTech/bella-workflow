package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.WorkflowScheduling.WORKFLOW_SCHEDULING;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import com.ke.bella.workflow.BellaContext;
import com.ke.bella.workflow.IDGenerator;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.db.tables.records.WorkflowSchedulingRecord;

@Component
public class WorkflowSchedulingRepo implements BaseRepo {

    @Resource
    private DSLContext db;

    public WorkflowSchedulingDB insertWorkflowScheduling(String workflowId, String cronExpression, LocalDateTime nextTriggerTime,
            String inputs) {
        WorkflowSchedulingRecord rec = WORKFLOW_SCHEDULING.newRecord();
        String schedulingId = IDGenerator.newWorkflowSchedulingId();

        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setWorkflowId(workflowId);
        rec.setWorkflowSchedulingId(schedulingId);
        rec.setCronExpression(cronExpression);
        rec.setTriggerNextTime(nextTriggerTime);
        rec.setInputs(inputs);

        fillCreatorInfo(rec);

        db.insertInto(WORKFLOW_SCHEDULING)
                .set(rec)
                .execute();

        return rec.into(WorkflowSchedulingDB.class);
    }

    public void updateWorkflowScheduling(WorkflowSchedulingDB schedulingDB) {
        WorkflowSchedulingRecord rec = WORKFLOW_SCHEDULING.newRecord();
        rec.from(schedulingDB);
        fillUpdatorInfo(rec);
        db.update(WORKFLOW_SCHEDULING)
                .set(rec)
                .where(WORKFLOW_SCHEDULING.WORKFLOW_SCHEDULING_ID.eq(schedulingDB.getWorkflowSchedulingId()))
                .execute();
    }

    public WorkflowSchedulingDB selectWorkflowScheduling(String workflowSchedulingId) {
        return db.selectFrom(WORKFLOW_SCHEDULING).where(WORKFLOW_SCHEDULING.WORKFLOW_SCHEDULING_ID.eq(workflowSchedulingId))
                .fetchOneInto(WorkflowSchedulingDB.class);
    }

    public List<WorkflowSchedulingDB> listWorkflowScheduling(LocalDateTime endTime, Set<String> status, Integer limit) {
        return db.selectFrom(WORKFLOW_SCHEDULING).where(WORKFLOW_SCHEDULING.TRIGGER_NEXT_TIME.le(endTime).and(WORKFLOW_SCHEDULING.STATUS.in(status)))
                .limit(limit).fetch()
                .into(WorkflowSchedulingDB.class);
    }
}
