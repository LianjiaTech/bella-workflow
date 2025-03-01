ALTER TABLE `kafka_datasource`
    ADD COLUMN `auto_offset_reset` VARCHAR(50) NOT NULL DEFAULT 'latest' COMMENT '偏移量重置策略：latest, earliest，默认为latest' AFTER `msg_schema`,
    ADD COLUMN `auth_config` TEXT COMMENT 'Kafka认证配置,存储为JSON格式,不为空表示需要认证' AFTER `auto_offset_reset`;
