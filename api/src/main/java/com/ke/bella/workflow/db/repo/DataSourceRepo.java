package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.KafkaDatasource.*;

import java.util.List;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceAdd;
import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceRm;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
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

}
