package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.WorkflowKafkaTrigger.*;
import static com.ke.bella.workflow.db.tables.WorkflowScheduling.*;
import static com.ke.bella.workflow.db.tables.WorkflowWebotTrigger.*;

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
import com.ke.bella.workflow.api.WorkflowOps.KafkaTriggerCreate;
import com.ke.bella.workflow.api.WorkflowOps.WebotTriggerCreate;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.db.tables.WorkflowKafkaTrigger;
import com.ke.bella.workflow.db.tables.pojos.WorkflowKafkaTriggerDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowWebotTriggerDB;
import com.ke.bella.workflow.db.tables.records.WorkflowKafkaTriggerRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowSchedulingRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowWebotTriggerRecord;
import com.ke.bella.workflow.utils.JsonUtils;

@Component
public class WorkflowTriggerRepo implements BaseRepo {
    public enum Type {
        SCHD,
        KFKA,
        WBOT;
    }

    @Resource
    private DSLContext db;

    public WorkflowSchedulingDB insertWorkflowScheduling(String workflowId, String cronExpression, LocalDateTime nextTriggerTime,
            String inputs) {
        WorkflowSchedulingRecord rec = WORKFLOW_SCHEDULING.newRecord();
        String triggerId = IDGenerator.newTriggerId(Type.SCHD.name());

        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setWorkflowId(workflowId);
        rec.setTriggerId(triggerId);
        rec.setWorkflowSchedulingId(triggerId);
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
                .where(WORKFLOW_SCHEDULING.TRIGGER_ID.eq(schedulingDB.getTriggerId()))
                .execute();
    }

    public WorkflowSchedulingDB selectWorkflowScheduling(String tenantId, String triggerId) {
        return db.selectFrom(WORKFLOW_SCHEDULING)
                .where(WORKFLOW_SCHEDULING.TENANT_ID.eq(tenantId)
                        .and(WORKFLOW_SCHEDULING.TRIGGER_ID.eq(triggerId)))
                .fetchOneInto(WorkflowSchedulingDB.class);
    }

    public List<WorkflowSchedulingDB> listWorkflowScheduling(LocalDateTime endTime, Set<String> status, Integer limit) {
        return db.selectFrom(WORKFLOW_SCHEDULING)
                .where(WORKFLOW_SCHEDULING.TRIGGER_NEXT_TIME.le(endTime)
                        .and(WORKFLOW_SCHEDULING.STATUS.in(status)))
                .limit(limit).fetch()
                .into(WorkflowSchedulingDB.class);
    }

    public Page<WorkflowSchedulingDB> pageWorkflowScheduling(WorkflowOps.WorkflowSchedulingPage op) {
        SelectSeekStep1<WorkflowSchedulingRecord, LocalDateTime> sql = db.selectFrom(WORKFLOW_SCHEDULING)
                .where(WORKFLOW_SCHEDULING.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(StringUtils.isEmpty(op.getTriggerId()) ? DSL.noCondition()
                        : WORKFLOW_SCHEDULING.TRIGGER_ID.eq(op.getTriggerId()))
                .and(StringUtils.isEmpty(op.getLastId()) ? DSL.noCondition()
                        : WORKFLOW_SCHEDULING.TRIGGER_ID.ge(op.getTriggerId()))
                .orderBy(WORKFLOW_SCHEDULING.MTIME.desc());
        return queryPage(db, sql, op.getPage(), op.getPageSize(), WorkflowSchedulingDB.class);
    }

    public List<WorkflowKafkaTriggerDB> listAllActiveKafkaTriggers(Set<String> datasourceIds) {
        return db.selectFrom(WORKFLOW_KAFKA_TRIGGER)
                .where(WORKFLOW_KAFKA_TRIGGER.DATASOURCE_ID.in(datasourceIds))
                .and(WORKFLOW_KAFKA_TRIGGER.STATUS.eq(0))
                .fetchInto(WorkflowKafkaTriggerDB.class);
    }

    public WorkflowKafkaTriggerDB addKafkaTrigger(KafkaTriggerCreate op) {
        WorkflowKafkaTriggerRecord rec = WorkflowKafkaTrigger.WORKFLOW_KAFKA_TRIGGER.newRecord();
        String triggerId = IDGenerator.newTriggerId(Type.KFKA.name());

        rec.setTenantId(op.getTenantId());
        rec.setDatasourceId(op.getDatasourceId());
        rec.setTriggerId(triggerId);
        rec.setInputs(JsonUtils.toJson(op.getInputs()));
        rec.setExpression(op.getExpression());
        rec.setWorkflowId(op.getWorkflowId());
        rec.setInputkey(op.getInputkey());

        fillCreatorInfo(rec);
        db.insertInto(WorkflowKafkaTrigger.WORKFLOW_KAFKA_TRIGGER).set(rec).execute();

        return rec.into(WorkflowKafkaTriggerDB.class);
    }

    public void deactiveKafkaTrigger(String triggerId) {
        WorkflowKafkaTriggerRecord rec = WorkflowKafkaTrigger.WORKFLOW_KAFKA_TRIGGER.newRecord();
        rec.setStatus(-1);
        fillUpdatorInfo(rec);

        db.update(WorkflowKafkaTrigger.WORKFLOW_KAFKA_TRIGGER)
                .set(rec)
                .where(WorkflowKafkaTrigger.WORKFLOW_KAFKA_TRIGGER.TRIGGER_ID.eq(triggerId))
                .execute();
    }

    public WorkflowKafkaTriggerDB queryKafkaTrigger(String triggerId) {
        return db.selectFrom(WORKFLOW_KAFKA_TRIGGER)
                .where(WORKFLOW_KAFKA_TRIGGER.TRIGGER_ID.eq(triggerId))
                .fetchOneInto(WorkflowKafkaTriggerDB.class);
    }

    public WorkflowWebotTriggerDB addWebotTrigger(WebotTriggerCreate op) {
        WorkflowWebotTriggerRecord rec = WORKFLOW_WEBOT_TRIGGER.newRecord();

        String triggerId = IDGenerator.newTriggerId(Type.WBOT.name());
        rec.setTenantId(op.getTenantId());
        rec.setTriggerId(triggerId);
        rec.setRobotId(op.getRobotId());
        if(StringUtils.hasText(op.getChatId())) {
            rec.setChatId(op.getChatId());
        }
        rec.setInputs(JsonUtils.toJson(op.getInputs()));
        rec.setExpression(op.getExpression());
        rec.setWorkflowId(op.getWorkflowId());
        rec.setInputkey(op.getInputkey());

        fillCreatorInfo(rec);

        db.insertInto(WORKFLOW_WEBOT_TRIGGER).set(rec).execute();

        return rec.into(WorkflowWebotTriggerDB.class);
    }

    public void deactiveWebotTrigger(String triggerId) {
        WorkflowWebotTriggerRecord rec = WORKFLOW_WEBOT_TRIGGER.newRecord();
        rec.setStatus(-1);
        fillUpdatorInfo(rec);

        db.update(WORKFLOW_WEBOT_TRIGGER)
                .set(rec)
                .where(WORKFLOW_WEBOT_TRIGGER.TRIGGER_ID.eq(triggerId))
                .execute();
    }

    public WorkflowWebotTriggerDB queryWebotTrigger(String triggerId) {
        return db.selectFrom(WORKFLOW_WEBOT_TRIGGER)
                .where(WORKFLOW_WEBOT_TRIGGER.TRIGGER_ID.eq(triggerId))
                .fetchOneInto(WorkflowWebotTriggerDB.class);
    }

    public List<WorkflowWebotTriggerDB> listAllActiveWebotTriggers(String robotId) {
        return db.selectFrom(WORKFLOW_WEBOT_TRIGGER)
                .where(WORKFLOW_WEBOT_TRIGGER.ROBOT_ID.eq(robotId))
                .and(WORKFLOW_KAFKA_TRIGGER.STATUS.eq(0))
                .fetchInto(WorkflowWebotTriggerDB.class);
    }
}
