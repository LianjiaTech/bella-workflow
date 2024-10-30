package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.KafkaDatasource.*;

import java.util.List;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceAdd;
import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceRm;
import com.ke.bella.workflow.api.WorkflowOps.DomainAdd;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.IDGenerator;
import static com.ke.bella.workflow.db.tables.Domain.*;
import com.ke.bella.workflow.db.tables.pojos.DomainDB;
import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
import com.ke.bella.workflow.db.tables.records.DomainRecord;
import com.ke.bella.workflow.db.tables.records.KafkaDatasourceRecord;

@Component
public class DataSourceRepo implements BaseRepo {

    @Resource
    private DSLContext db;

    public KafkaDatasourceDB addKafkaDs(KafkaDataSourceAdd op) {
        KafkaDatasourceRecord rec = KAFKA_DATASOURCE.newRecord();
        rec.from(op);
        rec.setDatasourceId(IDGenerator.newDataSourceId());
        if(!StringUtils.hasText(op.getMsgSchema())) {
            rec.setMsgSchema("");
        }

        fillCreatorInfo(rec);

        db.insertInto(KAFKA_DATASOURCE).set(rec).execute();

        return rec.into(KafkaDatasourceDB.class);
    }

    public void removeKafkaDs(KafkaDataSourceRm op) {
        KafkaDatasourceRecord rec = KAFKA_DATASOURCE.newRecord();
        rec.setStatus(-1);
        fillUpdatorInfo(rec);

        db.update(KAFKA_DATASOURCE)
                .set(rec)
                .where(KAFKA_DATASOURCE.DATASOURCE_ID.eq(op.getDatasourceID()))
                .execute();
    }

    public List<KafkaDatasourceDB> listAllKafkaDs() {
        return db.selectFrom(KAFKA_DATASOURCE)
                .fetchInto(KafkaDatasourceDB.class);
    }

    public List<KafkaDatasourceDB> listTenantAllActiveKafkaDs() {
        return db.selectFrom(KAFKA_DATASOURCE)
                .where(KAFKA_DATASOURCE.TENANT_ID.eq(BellaContext.getOperator().getTenantId())
                        .and(KAFKA_DATASOURCE.STATUS.eq(0)))
                .fetchInto(KafkaDatasourceDB.class);
    }

    public List<DomainDB> listDomains(String prefix) {
        return db.selectFrom(DOMAIN)
                .where(DOMAIN.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(DOMAIN.SPACE_CODE.eq(BellaContext.getOperator().getSpaceCode()))
                .and(StringUtils.isEmpty(prefix) ? DSL.noCondition() : DOMAIN.DOMAIN_.like(prefix + "%"))
                .fetchInto(DomainDB.class);
    }

    public DomainDB addDomain(DomainAdd domainOp) {
        DomainRecord rec = DOMAIN.newRecord();

        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setSpaceCode(BellaContext.getOperator().getSpaceCode());
        rec.setDomain(domainOp.getDomain());
        rec.setDesc(domainOp.getDesc());
        rec.setCustom(domainOp.getCustom());

        fillCreatorInfo(rec);

        db.insertInto(DOMAIN).set(rec).execute();

        return rec.into(DomainDB.class);
    }

    public List<String> listAllCustomDomains() {
        return db.select(DOMAIN.DOMAIN_)
                .from(DOMAIN)
                .where(DOMAIN.CUSTOM.eq(1))
                .fetch(DOMAIN.DOMAIN_);
    }

}
