package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.Tables.WORKFLOW_AGGREGATE;
import static com.ke.bella.workflow.db.tables.Tenant.TENANT;
import static com.ke.bella.workflow.db.tables.Workflow.WORKFLOW;
import static com.ke.bella.workflow.db.tables.WorkflowNodeRun.WORKFLOW_NODE_RUN;
import static com.ke.bella.workflow.db.tables.WorkflowRun.WORKFLOW_RUN;
import static com.ke.bella.workflow.db.tables.WorkflowRunSharding.WORKFLOW_RUN_SHARDING;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep1;
import org.jooq.SelectSeekStep2;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.BellaContext;
import com.ke.bella.workflow.IDGenerator;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAggregateDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunShardingDB;
import com.ke.bella.workflow.db.tables.records.TenantRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowAggregateRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowNodeRunRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowRunRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowRunShardingRecord;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
                .fetchOneInto(WorkflowDB.class);
    }

    public Page<WorkflowDB> pageDraftWorkflow(WorkflowPage op) {
        SelectSeekStep1<WorkflowRecord, Long> sql = db.selectFrom(WORKFLOW)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.VERSION.eq(0l))
                .and(StringUtils.isEmpty(op.getName()) ? DSL.noCondition() : WORKFLOW.TITLE.like("%" + op.getName() + "%"))
                .orderBy(WORKFLOW.ID.desc());
        return queryPage(db, sql, op.getPage(), op.getPageSize(), WorkflowDB.class);
    }

    public WorkflowRunDB queryDraftWorkflowRunSuccessed(WorkflowDB wf) {
        WorkflowRunShardingDB sharding = queryWorkflowRunShardingByTime(wf.getMtime());
        return db(sharding.getKey()).selectFrom(WORKFLOW_RUN)
                .where(WORKFLOW_RUN.TENANT_ID.eq(wf.getTenantId())
                        .and(WORKFLOW_RUN.WORKFLOW_ID.eq(wf.getWorkflowId()))
                        .and(WORKFLOW_RUN.WORKFLOW_VERSION.eq(wf.getVersion()))
                        .and(WORKFLOW_RUN.STATUS.eq("succeeded"))
                        .and(WORKFLOW_RUN.CTIME.ge(wf.getMtime())))
                .limit(1)
                .fetchOneInto(WorkflowRunDB.class);
    }

    public WorkflowDB queryPublishedWorkflow(String workflowId, Long version) {
        return db.selectFrom(WORKFLOW)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(workflowId))
                .and(version == null ? WORKFLOW.VERSION.greaterThan(0l) : WORKFLOW.VERSION.eq(version)) // 正式版
                .orderBy(WORKFLOW.VERSION.desc())   // 最新版
                .limit(1)
                .fetchOneInto(WorkflowDB.class);
    }

    public WorkflowDB queryWorkflow(String workflowId, Long version) {
        return db.selectFrom(WORKFLOW)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(workflowId))
                .and(WORKFLOW.VERSION.eq(version))
                .fetchOneInto(WorkflowDB.class);
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

    public WorkflowAggregateDB addWorkflowAggregate(WorkflowDB workflowDb) {
        WorkflowAggregateRecord rec = WORKFLOW_AGGREGATE.newRecord();
        rec.from(workflowDb);
        rec.setId(null);
        if(workflowDb.getVersion() > 0L) {
            rec.setLatestPublishVersion(workflowDb.getVersion());
        }
        db.insertInto(WORKFLOW_AGGREGATE).set(rec).execute();

        return rec.into(WorkflowAggregateDB.class);
    }

    public void updateWorkflowAggregate(WorkflowSync op) {
        WorkflowAggregateRecord rec = WORKFLOW_AGGREGATE.newRecord();
        if(!StringUtils.isEmpty(op.getGraph())) {
            rec.setGraph(op.getGraph());
        }
        if(!StringUtils.isEmpty(op.getTitle())) {
            rec.setTitle(op.getTitle());
        }
        if(!StringUtils.isEmpty(op.getDesc())) {
            rec.setDesc(op.getDesc());
        }
        fillUpdatorInfo(rec);

        int num = db.update(WORKFLOW_AGGREGATE)
                .set(rec)
                .where(WORKFLOW_AGGREGATE.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW_AGGREGATE.WORKFLOW_ID.eq(op.getWorkflowId()))
                .execute();
        Assert.isTrue(num == 1, "工作流聚合实体配置更新失败，请检查工作流聚合实体是否存在");
    }

    public void publishWorkflowAggregate(String workflowId, Long latestPublishVersion) {
        WorkflowAggregateRecord rec = WORKFLOW_AGGREGATE.newRecord();
        rec.setLatestPublishVersion(latestPublishVersion);
        fillUpdatorInfo(rec);

        int num = db.update(WORKFLOW_AGGREGATE)
                .set(rec)
                .where(WORKFLOW_AGGREGATE.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW_AGGREGATE.WORKFLOW_ID.eq(workflowId))
                .execute();
        Assert.isTrue(num == 1, "工作流聚合实体配置发布失败，请检查工作流聚合实体是否存在");
    }

    public void updateDraftWorkflow(WorkflowSync op) {
        WorkflowRecord rec = WORKFLOW.newRecord();
        if(!StringUtils.isEmpty(op.getGraph())) {
            rec.setGraph(op.getGraph());
        }
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

    public long publishWorkflow(String workflowId) {
        WorkflowRecord rec = WORKFLOW.newRecord();
        WorkflowDB wf = queryDraftWorkflow(workflowId);
        long version = System.currentTimeMillis();
        rec.from(wf);
        rec.setId(null);
        rec.setVersion(version);
        fillUpdatorInfo(rec);

        int num = db.insertInto(WORKFLOW).set(rec).execute();

        Assert.isTrue(num == 1, "工作流配置发布失败，请检查工作流配置版本是否为draft");
        return version;
    }

    public TenantDB addTenant(String tenantName, String parentTenantId) {
        TenantRecord rec = TENANT.newRecord();

        rec.setTenantId(IDGenerator.newTenantId());
        rec.setTenantName(tenantName);
        if(!StringUtils.isEmpty(parentTenantId)) {
            rec.setParentId(parentTenantId);
        }

        fillCreatorInfo(rec);

        db.insertInto(TENANT).set(rec).execute();

        return rec.into(TenantDB.class);
    }

    public List<TenantDB> listTenants(List<String> tenantId) {
        return db.selectFrom(TENANT).where(TENANT.TENANT_ID.in(tenantId)).fetch().into(TenantDB.class);
    }

    public WorkflowRunDB queryWorkflowRun(String workflowRunId) {
        String shardKey = shardingKeyByworkflowRunId(workflowRunId);
        return db(shardKey).selectFrom(WORKFLOW_RUN)
                .where(WORKFLOW_RUN.WORKFLOW_RUN_ID.eq(workflowRunId))
                .fetchOne().into(WorkflowRunDB.class);
    }

    public Page<WorkflowRunDB> listWorkflowRun(WorkflowRunPage op) {
        SelectConditionStep<WorkflowRunRecord> query = null;
        List<WorkflowRunShardingDB> shardings = queryWorkflowRunShardingsByTime(op.getStartTime(), op.getStartTime().plusDays(7));
        for (int i = 0; i < shardings.size(); i++) {
            WorkflowRunShardingDB sharding = shardings.get(i);
            SelectConditionStep<WorkflowRunRecord> sql = db(sharding.getKey()).selectFrom(WORKFLOW_RUN)
                    .where(WORKFLOW_RUN.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                    .and(WORKFLOW_RUN.WORKFLOW_ID.eq(op.getWorkflowId()))
                    .and(StringUtils.isEmpty(op.getLastId()) ? DSL.noCondition() : WORKFLOW_RUN.WORKFLOW_RUN_ID.ge(op.getLastId()));
            if(i == 0) {
                query = sql;
            } else {
                query.unionAll(sql);
            }
        }

        return queryPage(db, query, op.getPage(), op.getPageSize(), WorkflowRunDB.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowRunDB addWorkflowRun(WorkflowDB wf, WorkflowRun op, String inputs) {
        WorkflowRunRecord rec = WORKFLOW_RUN.newRecord();

        String runId = IDGenerator.newWorkflowRunId();
        String shardKey = shardingKeyByworkflowRunId(runId);

        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setWorkflowId(wf.getWorkflowId());
        rec.setWorkflowVersion(wf.getVersion());
        rec.setWorkflowRunId(runId);
        rec.setWorkflowRunShardingKey(shardKey);
        rec.setInputs(inputs);
        rec.setOutputs("");
        rec.setError("");
        rec.setTriggerFrom(op.getTriggerFrom());
        if(op.getCallbackUrl() != null) {
            rec.setCallbackUrl(op.getCallbackUrl());
        }
        if(op.getResponseMode() != null) {
            rec.setResponseMode(op.getResponseMode());
        }
        if(StringUtils.isEmpty(op.getTraceId())) {
            rec.setTraceId(runId);
        } else {
            rec.setTraceId(op.getTraceId());
        }

        rec.setSpanLev(op.getSpanLev());

        fillCreatorInfo(rec);

        db(shardKey).insertInto(WORKFLOW_RUN)
                .set(rec)
                .execute();

        return rec.into(WorkflowRunDB.class);
    }

    public void updateWorkflowRun(WorkflowRunDB wr) {
        WorkflowRunRecord rec = WORKFLOW_RUN.newRecord();
        rec.from(wr);
        fillUpdatorInfo(rec);

        String shardKey = shardingKeyByworkflowRunId(wr.getWorkflowRunId());
        db(shardKey).update(WORKFLOW_RUN)
                .set(rec)
                .where(WORKFLOW_RUN.WORKFLOW_RUN_ID.eq(wr.getWorkflowRunId()))
                .execute();
    }

    public WorkflowRunShardingDB queryWorkflowRunShardingByRunID(String workflowRunId) {
        LocalDateTime time = IDGenerator.timeFromCode(workflowRunId);
        return db.selectFrom(WORKFLOW_RUN_SHARDING)
                .where(WORKFLOW_RUN_SHARDING.KEY_TIME.le(time))
                .orderBy(WORKFLOW_RUN_SHARDING.ID.desc())
                .limit(1)
                .fetchOneInto(WorkflowRunShardingDB.class);
    }

    public WorkflowRunShardingDB queryWorkflowRunShardingByTime(LocalDateTime time) {
        return db.selectFrom(WORKFLOW_RUN_SHARDING)
                .where(WORKFLOW_RUN_SHARDING.KEY_TIME.le(time))
                .orderBy(WORKFLOW_RUN_SHARDING.ID.desc())
                .limit(1)
                .fetchOneInto(WorkflowRunShardingDB.class);
    }

    public List<WorkflowRunShardingDB> queryWorkflowRunShardingsByTime(LocalDateTime startTime, LocalDateTime endTime) {
        return db.selectFrom(WORKFLOW_RUN_SHARDING)
                .where(WORKFLOW_RUN_SHARDING.KEY_TIME.ge(startTime)
                        .and(WORKFLOW_RUN_SHARDING.KEY_TIME.le(endTime)))
                .orderBy(WORKFLOW_RUN_SHARDING.ID.desc())
                .unionAll(
                        db.selectFrom(WORKFLOW_RUN_SHARDING)
                                .where(WORKFLOW_RUN_SHARDING.KEY_TIME.le(startTime))
                                .orderBy(WORKFLOW_RUN_SHARDING.ID.desc())
                                .limit(1))
                .fetchInto(WorkflowRunShardingDB.class);
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
        db.execute(createTableLikeSql(WORKFLOW_NODE_RUN.getName(), key));

        addWorkflowSharding(keyTime, lastKey, key);
    }

    private static String createTableLikeSql(String tableName, String key) {
        return String.format("create table `%s_%s` like `%s`", tableName, key, tableName);
    }

    public void addWorkflowRunNode(WorkflowNodeRunDB wnr) {
        WorkflowNodeRunRecord rec = WORKFLOW_NODE_RUN.newRecord();
        rec.from(wnr);

        fillCreatorInfo(rec);

        String shardKey = shardingKeyByworkflowRunId(wnr.getWorkflowRunId());
        db(shardKey).insertInto(WORKFLOW_NODE_RUN)
                .set(rec)
                .execute();
    }

    public void updateWorkflowNodeRun(WorkflowNodeRunDB wnr) {
        WorkflowNodeRunRecord rec = WORKFLOW_NODE_RUN.newRecord();
        rec.from(wnr);

        fillUpdatorInfo(rec);
        String shardKey = shardingKeyByworkflowRunId(wnr.getWorkflowRunId());
        db(shardKey).update(WORKFLOW_NODE_RUN)
                .set(rec)
                .where(WORKFLOW_NODE_RUN.TENANT_ID.eq(wnr.getTenantId()))
                .and(WORKFLOW_NODE_RUN.WORKFLOW_ID.eq(wnr.getWorkflowId()))
                .and(WORKFLOW_NODE_RUN.WORKFLOW_RUN_ID.eq(wnr.getWorkflowRunId()))
                .and((WORKFLOW_NODE_RUN.NODE_ID.eq(wnr.getNodeId())))
                .and(WORKFLOW_NODE_RUN.NODE_RUN_ID.eq(wnr.getNodeRunId()))
                .execute();
    }

    public List<WorkflowNodeRunDB> queryWorkflowNodeRuns(String workflowRunId, Set<String> nodeids) {
        String shardKey = shardingKeyByworkflowRunId(workflowRunId);
        return db(shardKey).selectFrom(WORKFLOW_NODE_RUN)
                .where(WORKFLOW_NODE_RUN.WORKFLOW_RUN_ID.eq(workflowRunId)
                        .and(WORKFLOW_NODE_RUN.NODE_ID.in(nodeids)))
                .fetchInto(WorkflowNodeRunDB.class);
    }

    public List<WorkflowNodeRunDB> queryWorkflowNodeRuns(String workflowRunId) {
        String shardKey = shardingKeyByworkflowRunId(workflowRunId);
        return db(shardKey).selectFrom(WORKFLOW_NODE_RUN)
                .where(WORKFLOW_NODE_RUN.WORKFLOW_RUN_ID.eq(workflowRunId))
                .fetchInto(WorkflowNodeRunDB.class);
    }

    public Page<WorkflowDB> pageWorkflows(WorkflowPage op) {
        SelectSeekStep1<WorkflowRecord, LocalDateTime> sql = db.selectFrom(WORKFLOW)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(StringUtils.isEmpty(op.getName()) ? DSL.noCondition() : WORKFLOW.TITLE.like("%" + op.getName() + "%"))
                .and(StringUtils.isEmpty(op.getWorkflowId()) ? DSL.noCondition() : WORKFLOW.WORKFLOW_ID.eq(op.getWorkflowId()))
                .orderBy(WORKFLOW.MTIME.desc());
        return queryPage(db, sql, op.getPage(), op.getPageSize(), WorkflowDB.class);
    }

    public Page<WorkflowAggregateDB> pageWorkflowAggregate(WorkflowPage op) {
        SelectSeekStep2<WorkflowAggregateRecord, Integer, LocalDateTime> sql = db.selectFrom(WORKFLOW_AGGREGATE)
                .where(WORKFLOW_AGGREGATE.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(Objects.isNull(BellaContext.getOperator().getUserId()) ? DSL.noCondition()
                        : WORKFLOW_AGGREGATE.CUID.eq(BellaContext.getOperator().getUserId()))
                .and(StringUtils.isEmpty(op.getName()) ? DSL.noCondition()
                        : WORKFLOW_AGGREGATE.TITLE.like("%" + DSL.escape(op.getName(), '\\') + "%"))
                .and(StringUtils.isEmpty(op.getWorkflowId()) ? DSL.noCondition() : WORKFLOW_AGGREGATE.WORKFLOW_ID.eq(op.getWorkflowId()))
                .orderBy(DSL.when(WORKFLOW_AGGREGATE.LATEST_PUBLISH_VERSION.gt(0L), 0).otherwise(1).asc(), WORKFLOW_AGGREGATE.MTIME.desc());
        return queryPage(db, sql, op.getPage(), op.getPageSize(), WorkflowAggregateDB.class);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowWithPublished extends WorkflowDB {
        /**
         * 曾经发布过
         */
        private Boolean hasPublished;
    }
}
