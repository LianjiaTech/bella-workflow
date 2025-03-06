ALTER TABLE `kafka_datasource`
    ADD COLUMN `auto_offset_reset` VARCHAR(50) NOT NULL DEFAULT 'latest' COMMENT '偏移量重置策略：latest, earliest，默认为latest' AFTER `msg_schema`,
    ADD COLUMN `props_config` TEXT COMMENT 'Kafka属性配置信息，存储为JSON格式，例如：认证、超时等参数' AFTER `auto_offset_reset`;
