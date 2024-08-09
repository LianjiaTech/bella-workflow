package com.ke.bella.workflow.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
import com.ke.bella.workflow.service.DataSourceService;

@RestController
@RequestMapping("/console/api/datasource")
public class DataSourceController {

    @Autowired
    DataSourceService ds;

    @PostMapping("/kafka/create")
    public KafkaDatasourceDB createKafkaDs(@RequestBody DataSourceOps.KafkaDataSourceAdd op) {
        return ds.createKafkaDs(op);
    }

    @PostMapping("/kafka/remove")
    public void removeKafkaDs(@RequestBody DataSourceOps.KafkaDataSourceRm op) {
        ds.removeKafkaDs(op);
    }
}
