package com.ke.bella.workflow.api;

import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
import com.ke.bella.workflow.db.tables.pojos.RdbDatasourceDB;
import com.ke.bella.workflow.db.tables.pojos.RedisDatasourceDB;
import com.ke.bella.workflow.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public void removeKafkaDs(@RequestBody DataSourceOps.DataSourceOp op) {
        ds.removeKafkaDs(op);
    }

    @PostMapping("/rdb/create")
    public RdbDatasourceDB createRdb(@RequestBody DataSourceOps.RdbDataSourceAdd op) {
        return ds.createRdbDatasource(op);
    }

    @PostMapping("/rdb/remove")
    public void removeRdb(@RequestBody DataSourceOps.DataSourceOp op) {
        ds.removeRdbDatasource(op);
    }

    @PostMapping("/rdb/check")
    public void checkRdb(@RequestBody DataSourceOps.RdbDataSourceAdd op) {
        ds.checkRdbDatasource(op);
    }

    @PostMapping("/redis/create")
    public RedisDatasourceDB createRedis(@RequestBody DataSourceOps.RedisDataSourceAdd op) {
        return ds.createRedisDatasource(op);
    }

    @PostMapping("/redis/remove")
    public void removeRedis(@RequestBody DataSourceOps.DataSourceOp op) {
        ds.removeRedisDatasource(op);
    }

    @PostMapping("/redis/check")
    public void checkRedis(@RequestBody DataSourceOps.RedisDataSourceAdd op) {
        ds.checkRedisDatasource(op);
    }
}
