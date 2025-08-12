ALTER TABLE `kafka_datasource`
    ADD COLUMN `group_id` varchar(255) NOT NULL DEFAULT '' COMMENT '消费者组id' AFTER `name`;
