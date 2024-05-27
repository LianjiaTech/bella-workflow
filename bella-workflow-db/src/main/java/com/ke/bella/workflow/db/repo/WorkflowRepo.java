package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.Tenant.*;
import static com.ke.bella.workflow.db.tables.Workflow.*;
import static com.ke.bella.workflow.db.tables.WorkflowRun.*;
import static com.ke.bella.workflow.db.tables.WorkflowRunSharding.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.BellaContext;
import com.ke.bella.workflow.IDGenerator;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.db.tables.WorkflowNodeRun;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunShardingDB;
import com.ke.bella.workflow.db.tables.records.TenantRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowRunRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowRunShardingRecord;

@Component
public class WorkflowRepo implements BaseRepo {

    @Resource
    private DSLContext db;

    private DSLContext db(String shardingKey) {
        return DSLContextHolder.get(shardingKey, db);
    }

    private String shardingKeyByworkflowRunId(String workflowRunId) {
        WorkflowRunShardingDB s = queryWorkflowRunShardingByRunID(workflowRunId);
        return s.getKey();
    }

    public WorkflowDB queryDraftWorkflow(String workflowId) {
        return db.selectFrom(WORKFLOW)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(workflowId))
                .and(WORKFLOW.VERSION.eq(0l))
                .fetchOne().into(WorkflowDB.class);
    }

    public WorkflowRunDB queryDraftWorkflowRunSuccessed(WorkflowDB wf) {
        return db.selectFrom(WORKFLOW_RUN)
                .where(WORKFLOW_RUN.TENANT_ID.eq(wf.getTenantId())
                        .and(WORKFLOW_RUN.WORKFLOW_ID.eq(wf.getWorkflowId()))
                        .and(WORKFLOW_RUN.WORKFLOW_VERSION.eq(wf.getVersion()))
                        .and(WORKFLOW_RUN.STATUS.eq("successed"))
                        .and(WORKFLOW_RUN.CTIME.ge(wf.getMtime())))
                .limit(1)
                .fetchOneInto(WorkflowRunDB.class);
    }

    public WorkflowDB queryPublishedWorkflow(String workflowId) {
        return db.selectFrom(WORKFLOW)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(workflowId))
                .and(WORKFLOW.VERSION.greaterThan(0l)) // 正式版
                .orderBy(WORKFLOW.VERSION.desc())   // 最新版
                .limit(1)
                .fetchOneInto(WorkflowDB.class);
    }

    public WorkflowDB queryWorkflow(String workflowId, Long version) {
        return db.selectFrom(WORKFLOW)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(workflowId))
                .and(WORKFLOW.VERSION.eq(version))
                .fetchOne().into(WorkflowDB.class);
    }

    public WorkflowDB addDraftWorkflow(WorkflowSync op) {
        WorkflowRecord rec = WORKFLOW.newRecord();

        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setWorkflowId(op.getWorkflowId() == null ? IDGenerator.newWorkflowId() : op.getWorkflowId());
        rec.setGraph(op.getGraph());
        if(!StringUtils.isEmpty(op.getTitle())) {
            rec.setTitle(op.getTitle());
        }
        if(!StringUtils.isEmpty(op.getDesc())) {
            rec.setDesc(op.getDesc());
        }
        rec.setVersion(0L);

        fillCreatorInfo(rec);

        db.insertInto(WORKFLOW).set(rec).execute();

        return rec.into(WorkflowDB.class);
    }

    public void updateDraftWorkflow(WorkflowSync op) {
        WorkflowRecord rec = WORKFLOW.newRecord();
        rec.setGraph(op.getGraph());
        if(!StringUtils.isEmpty(op.getTitle())) {
            rec.setTitle(op.getTitle());
        }
        if(!StringUtils.isEmpty(op.getDesc())) {
            rec.setDesc(op.getDesc());
        }
        fillUpdatorInfo(rec);

        int num = db.update(WORKFLOW)
                .set(rec)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(op.getWorkflowId()))
                .and(WORKFLOW.VERSION.eq(0L))
                .execute();
        Assert.isTrue(num == 1, "工作流配置更新失败，请检查工作流配置版本是否为draft");
    }

    public void publishWorkflow(String workflowId) {
        WorkflowRecord rec = WORKFLOW.newRecord();
        rec.setVersion(System.currentTimeMillis());
        fillUpdatorInfo(rec);

        int num = db.update(WORKFLOW)
                .set(rec)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(workflowId))
                .and(WORKFLOW.VERSION.eq(0L))
                .execute();
        Assert.isTrue(num == 1, "工作流配置发布失败，请检查工作流配置版本是否为draft");
    }

    public TenantDB addTenant(String tenantName) {
        TenantRecord rec = TENANT.newRecord();

        rec.setTenantId(IDGenerator.newTenantId());
        rec.setTenantName(tenantName);

        fillCreatorInfo(rec);

        db.insertInto(TENANT).set(rec).execute();

        return rec.into(TenantDB.class);
    }

    public Page<WorkflowRunDB> pageWorkflowRun(String workflowId, String status) {
        SelectSeekStep1<WorkflowRunRecord, LocalDateTime> query = db.selectFrom(WORKFLOW_RUN)
                .where(WORKFLOW_RUN.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW_RUN.WORKFLOW_ID.eq(workflowId))
                .and(status != null ? WORKFLOW_RUN.STATUS.eq(status) : DSL.noCondition())
                .orderBy(WORKFLOW_RUN.CTIME.desc());
        return queryPage(db, query, 0, 0, WorkflowRunDB.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowRunDB addWorkflowRun(WorkflowDB wf, String inputs, String callbackUrl) {
        WorkflowRunRecord rec = WORKFLOW_RUN.newRecord();

        String runId = IDGenerator.newWorkflowRunId();
        String shardKey = shardingKeyByworkflowRunId(runId);

        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setWorkflowId(wf.getWorkflowId());
        rec.setWorkflowVersion(wf.getVersion());
        rec.setWorkflowRunId(runId);
        rec.setWorkflowRunShardingKey(shardKey);
        rec.setInputs(inputs);
        if(callbackUrl != null) {
            rec.setCallbackUrl(callbackUrl);
        }

        fillCreatorInfo(rec);

        db(shardKey).insertInto(WORKFLOW_RUN)
                .set(rec)
                .execute();

        return rec.into(WorkflowRunDB.class);
    }

    public WorkflowRunShardingDB queryWorkflowRunShardingByRunID(String workflowRunId) {
        LocalDateTime time = IDGenerator.timeFromCode(workflowRunId);
        return db.selectFrom(WORKFLOW_RUN_SHARDING)
                .where(WORKFLOW_RUN_SHARDING.KEY_TIME.le(time))
                .orderBy(WORKFLOW_RUN_SHARDING.ID.desc())
                .limit(1)
                .fetchOneInto(WorkflowRunShardingDB.class);
    }

    public WorkflowRunShardingDB queryWorkflowRunShardingByKey(String key) {
        return db.selectFrom(WORKFLOW_RUN_SHARDING)
                .where(WORKFLOW_RUN_SHARDING.KEY.eq(key))
                .fetchOneInto(WorkflowRunShardingDB.class);
    }

    public WorkflowRunShardingDB queryLatestWorkflowRunSharding() {
        return db.selectFrom(WORKFLOW_RUN_SHARDING)
                .orderBy(WORKFLOW_RUN_SHARDING.ID.desc())
                .limit(1)
                .fetchOneInto(WorkflowRunShardingDB.class);
    }

    private void addWorkflowSharding(LocalDateTime keyTime, String lastKey, String key) {
        WorkflowRunShardingRecord rec = WORKFLOW_RUN_SHARDING.newRecord();
        rec.setKey(key);
        rec.setKeyTime(keyTime);
        rec.setLastKey(lastKey);
        fillCreatorInfo(rec);

        db.insertInto(WORKFLOW_RUN_SHARDING)
                .set(rec)
                .execute();
    }

    public void increaseShardingCount(String key, long delta) {
        db.update(WORKFLOW_RUN_SHARDING)
                .set(WORKFLOW_RUN_SHARDING.COUNT, WORKFLOW_RUN_SHARDING.COUNT.plus(delta))
                .set(WORKFLOW_RUN_SHARDING.MTIME, LocalDateTime.now())
                .where(WORKFLOW_RUN_SHARDING.KEY.eq(key))
                .execute();
    }

    @Transactional(rollbackFor = Exception.class)
    public void newShardingTable(String lastKey) {
        LocalDateTime keyTime = LocalDateTime.now().plusMinutes(10L);
        String key = keyTime.format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));

        WorkflowRunShardingRecord rec = db.selectFrom(WORKFLOW_RUN_SHARDING)
                .where(WORKFLOW_RUN_SHARDING.LAST_KEY.eq(lastKey)).forUpdate().fetchOne();
        if(rec != null) {
            return;
        }

        db.execute(createTableLikeSql(WORKFLOW_RUN.getName(), key));
        db.execute(createTableLikeSql(WorkflowNodeRun.WORKFLOW_NODE_RUN.getName(), key));

        addWorkflowSharding(keyTime, lastKey, key);
    }

    private static String createTableLikeSql(String tableName, String key) {
        return String.format("create table `%s_%s` like `%s`", tableName, key, tableName);
    }
}
