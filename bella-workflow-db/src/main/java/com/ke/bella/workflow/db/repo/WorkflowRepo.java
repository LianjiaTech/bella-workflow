package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.Tenant.*;
import static com.ke.bella.workflow.db.tables.Workflow.*;
import static com.ke.bella.workflow.db.tables.WorkflowRun.*;

import java.time.LocalDateTime;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;
import org.springframework.util.Assert;

import com.ke.bella.workflow.BellaContext;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.records.TenantRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowRecord;
import com.ke.bella.workflow.db.tables.records.WorkflowRunRecord;

public class WorkflowRepo implements BaseRepo {

    @Resource
    private DSLContext db;

    public WorkflowDB queryWorkflow(String workflowId) {
        return db.selectFrom(WORKFLOW)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(workflowId))
                .fetchOne().into(WorkflowDB.class);
    }

    public WorkflowDB addWorkflow(String graph) {
        WorkflowRecord rec = WORKFLOW.newRecord();

        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setWorkflowId(""); // TODO
        rec.setGraph(graph);
        rec.setVersion(0L);

        fillCreatorInfo(rec);

        db.insertInto(WORKFLOW).set(rec).execute();

        return rec.into(WorkflowDB.class);
    }

    public void syncWorkflow(String workflowId, String graph) {
        WorkflowRecord rec = WORKFLOW.newRecord();
        rec.setGraph(graph);
        fillUpdatorInfo(rec);

        int num = db.update(WORKFLOW)
                .set(rec)
                .where(WORKFLOW.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WORKFLOW.WORKFLOW_ID.eq(workflowId))
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

        rec.setTenantId(""); // TODO
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
}
