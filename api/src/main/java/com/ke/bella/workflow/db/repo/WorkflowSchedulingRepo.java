package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.WorkflowScheduling.WORKFLOW_SCHEDULING;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.IDGenerator;
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

    public WorkflowSchedulingDB selectWorkflowScheduling(String tenantId, String workflowSchedulingId) {
        return db.selectFrom(WORKFLOW_SCHEDULING)
                .where(WORKFLOW_SCHEDULING.TENANT_ID.eq(tenantId).and(WORKFLOW_SCHEDULING.WORKFLOW_SCHEDULING_ID.eq(workflowSchedulingId)))
                .fetchOneInto(WorkflowSchedulingDB.class);
    }

    public List<WorkflowSchedulingDB> listWorkflowScheduling(LocalDateTime endTime, Set<String> status, Integer limit) {
        return db.selectFrom(WORKFLOW_SCHEDULING).where(WORKFLOW_SCHEDULING.TRIGGER_NEXT_TIME.le(endTime).and(WORKFLOW_SCHEDULING.STATUS.in(status)))
                .limit(limit).fetch()
                .into(WorkflowSchedulingDB.class);
    }

    public Page<WorkflowSchedulingDB> pageWorkflowScheduling(WorkflowOps.WorkflowSchedulingPage op) {
        SelectSeekStep1<WorkflowSchedulingRecord, LocalDateTime> sql = db.selectFrom(WORKFLOW_SCHEDULING)
                .where(WORKFLOW_SCHEDULING.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(StringUtils.isEmpty(op.getWorkflowSchedulingId()) ? DSL.noCondition()
                        : WORKFLOW_SCHEDULING.WORKFLOW_SCHEDULING_ID.eq(op.getWorkflowSchedulingId()))
                .and(StringUtils.isEmpty(op.getLastId()) ? DSL.noCondition()
                        : WORKFLOW_SCHEDULING.WORKFLOW_SCHEDULING_ID.ge(op.getWorkflowSchedulingId()))
                .orderBy(WORKFLOW_SCHEDULING.MTIME.desc());
        return queryPage(db, sql, op.getPage(), op.getPageSize(), WorkflowSchedulingDB.class);
    }
}
