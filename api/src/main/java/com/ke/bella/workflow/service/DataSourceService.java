package com.ke.bella.workflow.service;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceAdd;
import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceRm;
import com.ke.bella.workflow.db.repo.DataSourceRepo;
import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;

@Component
public class DataSourceService {

    @Resource
    DataSourceRepo repo;

    public KafkaDatasourceDB createKafkaDs(KafkaDataSourceAdd op) {
        return repo.addKafkaDs(op);
    }

    public void removeKafkaDs(KafkaDataSourceRm op) {
        repo.removeKafkaDs(op);
    }

    public Object listDataSources(String type) {
        if("kafka".equals(type)) {
            return repo.listTenantAllActiveKafkaDs();
        }
        return null;
    }

}
