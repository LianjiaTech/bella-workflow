package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.Tables.REDIS_DATASOURCE;
import static com.ke.bella.workflow.db.tables.KafkaDatasource.*;

import java.util.List;

import javax.annotation.Resource;

import com.ke.bella.workflow.db.tables.pojos.RedisDatasourceDB;
import com.ke.bella.workflow.db.tables.records.RedisDatasourceRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.api.DataSourceOps.DataSourceOp;
import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceAdd;
import com.ke.bella.workflow.api.DataSourceOps.RdbDataSourceAdd;
import com.ke.bella.workflow.api.DataSourceOps.RedisDataSourceAdd;
import com.ke.bella.workflow.api.WorkflowOps.DomainAdd;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.IDGenerator;
import static com.ke.bella.workflow.db.tables.Domain.*;
import static com.ke.bella.workflow.db.tables.RdbDatasource.*;
import com.ke.bella.workflow.db.tables.pojos.DomainDB;
import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
import com.ke.bella.workflow.db.tables.pojos.RdbDatasourceDB;
import com.ke.bella.workflow.db.tables.records.DomainRecord;
import com.ke.bella.workflow.db.tables.records.KafkaDatasourceRecord;
import com.ke.bella.workflow.db.tables.records.RdbDatasourceRecord;
import com.ke.bella.workflow.utils.JsonUtils;

@Component
public class DataSourceRepo implements BaseRepo {

    @Resource
    private DSLContext db;

    public KafkaDatasourceDB queryKafkaDs(String dataSourceId, String type) {
        return db.selectFrom(KAFKA_DATASOURCE)
                .where(KAFKA_DATASOURCE.DATASOURCE_ID.eq(dataSourceId))
                .and(KAFKA_DATASOURCE.STATUS.eq(0))
                .and(KAFKA_DATASOURCE.TYPE.eq(type))
                .fetchOneInto(KafkaDatasourceDB.class);
    }

    public KafkaDatasourceDB addKafkaDs(KafkaDataSourceAdd op) {
        KafkaDatasourceRecord rec = KAFKA_DATASOURCE.newRecord();
        rec.from(op);
        rec.setDatasourceId(IDGenerator.newDataSourceId("kafka"));
        if(!StringUtils.hasText(op.getMsgSchema())) {
            rec.setMsgSchema("");
        }

        fillCreatorInfo(rec);

        db.insertInto(KAFKA_DATASOURCE).set(rec).execute();

        return rec.into(KafkaDatasourceDB.class);
    }

    public void removeKafkaDs(DataSourceOp op) {
        KafkaDatasourceRecord rec = KAFKA_DATASOURCE.newRecord();
        rec.setStatus(-1);
        fillUpdatorInfo(rec);

        db.update(KAFKA_DATASOURCE)
                .set(rec)
                .where(KAFKA_DATASOURCE.DATASOURCE_ID.eq(op.getDatasourceId()))
                .execute();
    }

    public List<KafkaDatasourceDB> listAllConsumerKafkaDs() {
        return db.selectFrom(KAFKA_DATASOURCE)
                .where(KAFKA_DATASOURCE.TYPE.eq("consumer"))
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

    public RdbDatasourceDB addRdbDataSource(RdbDataSourceAdd op) {
        RdbDatasourceRecord rec = RDB_DATASOURCE.newRecord();

        rec.setDatasourceId(IDGenerator.newDataSourceId(op.getDbType()));
        rec.setTenantId(op.getTenantId());
        rec.setSpaceCode(op.getSpaceCode());
        rec.setHost(op.getHost());
        rec.setPort(op.getPort());
        rec.setDb(op.getDb());
        rec.setUser(op.getUser());
        rec.setPassword(op.getPassword());
        rec.setParams(JsonUtils.toJson(op.getParams()));
        rec.setDbType(op.getDbType());

        fillCreatorInfo(rec);

        db.insertInto(RDB_DATASOURCE).set(rec).execute();

        return rec.into(RdbDatasourceDB.class);
    }

    public void removeRdbDatasource(DataSourceOp op) {
        RdbDatasourceRecord rec = RDB_DATASOURCE.newRecord();
        rec.setStatus(-1);
        fillUpdatorInfo(rec);

        db.update(RDB_DATASOURCE)
                .set(rec)
                .where(RDB_DATASOURCE.DATASOURCE_ID.eq(op.getDatasourceId()))
                .execute();

    }

    public RdbDatasourceDB queryRdbDataSource(String datasourceId) {
        return db.selectFrom(RDB_DATASOURCE)
                .where(RDB_DATASOURCE.DATASOURCE_ID.eq(datasourceId))
                .and(RDB_DATASOURCE.STATUS.eq(0))
                .fetchOneInto(RdbDatasourceDB.class);
    }

    public RedisDatasourceDB addRedisDataSource(RedisDataSourceAdd op) {
        RedisDatasourceRecord rec = REDIS_DATASOURCE.newRecord();

        rec.from(op);
        rec.setDatasourceId(IDGenerator.newDataSourceId("redis"));

        fillCreatorInfo(rec);

        db.insertInto(REDIS_DATASOURCE).set(rec).execute();

        return rec.into(RedisDatasourceDB.class);
    }

    public void removeRedisDatasource(DataSourceOp op) {
        RedisDatasourceRecord rec = REDIS_DATASOURCE.newRecord();
        rec.setStatus(-1);
        fillUpdatorInfo(rec);

        db.update(REDIS_DATASOURCE)
                .set(rec)
                .where(REDIS_DATASOURCE.DATASOURCE_ID.eq(op.getDatasourceId()))
                .execute();

    }

    public RedisDatasourceDB queryRedisDataSource(String datasourceId) {
        return db.selectFrom(REDIS_DATASOURCE)
                .where(REDIS_DATASOURCE.DATASOURCE_ID.eq(datasourceId))
                .and(REDIS_DATASOURCE.STATUS.eq(0))
                .fetchOneInto(RedisDatasourceDB.class);
    }

}
