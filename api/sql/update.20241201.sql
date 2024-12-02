alter table `kafka_datasource`
    add column `type` varchar(16) not null default 'consumer' comment 'kafka数据源类型\producer, consumer' after space_code;
