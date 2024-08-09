SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `wecom_group_info`
(
    `id`          BIGINT ( 20 ) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '租户ID',
    `space_code`  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '空间编码，默认：personal，.......',
    `group_code`  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '群编码-暗号',
    `group_name`  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '群名字',
    `group_alias` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '群备注',
    `group_id`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '企微中台ID-虚拟号指定群发消息',
    `chat_id`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '会话Id-群Id-机器人指定群发消息',
    `cuid`        BIGINT ( 20 ) NOT NULL DEFAULT '0' COMMENT '创建人ucid',
    `cu_name`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '创建人名字',
    `muid`        BIGINT ( 20 ) NOT NULL DEFAULT '0' COMMENT '修改人ucid',
    `mu_name`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '修改人名字',
    `status`      TINYINT ( 4 ) NOT NULL DEFAULT '0' COMMENT '记录状态（0:正常, -1:已删除）',
    `ctime`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `mtime`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_code` ( `group_code` ),
    KEY           `idx_tenant_id_space_code_cuid` ( `tenant_id`, `space_code`, `cuid` )
) ENGINE = INNODB AUTO_INCREMENT = 0 DEFAULT CHARSET = utf8mb4 COMMENT = '企业微信群信息管理';

CREATE TABLE `wecom_group_member`
(
    `id`            BIGINT ( 20 ) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '租户ID',
    `space_code`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '空间编码，默认：personal，.......',
    `group_code`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '群编码-暗号',
    `user_code`     VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '群成员系统号',
    `robot_id`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '机器人ID',
    `name`          VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '名称',
    `robot_webhook` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '机器人钩子地址',
    `type`          TINYINT ( 4 ) NOT NULL DEFAULT '0' COMMENT '成员类型（0:未知,1:虚拟账号,2:机器人,3:真实用户）',
    `cuid`          BIGINT ( 20 ) NOT NULL DEFAULT '0' COMMENT '创建人ucid',
    `cu_name`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '创建人名字',
    `muid`          BIGINT ( 20 ) NOT NULL DEFAULT '0' COMMENT '修改人ucid',
    `mu_name`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '修改人名字',
    `status`        TINYINT ( 4 ) NOT NULL DEFAULT '0' COMMENT '记录状态（0:正常, -1:已删除）',
    `ctime`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `mtime`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY             `idx_tenant_id_space_code_cuid_group_code` ( `tenant_id`, `space_code`, `group_code` )
) ENGINE = INNODB AUTO_INCREMENT = 0 DEFAULT CHARSET = utf8mb4 COMMENT = '企业微信群成员信息';


DROP TABLE IF EXISTS `kafka_datasource`;
CREATE TABLE `kafka_datasource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户ID',
  `datasource_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `space_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PERSONNAL',
  `server` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'kafka服务地址\nhost:port',
  `topic` varchar(255) NOT NULL COMMENT 'Kafka topic',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `msg_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息体的json schema',
  `status` int NOT NULL DEFAULT '0' COMMENT '数据源状态\n-1: 无笑\n0: 生效',
  `cuid` bigint NOT NULL DEFAULT '0',
  `muid` bigint NOT NULL DEFAULT '0',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cu_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `mu_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_id` (`datasource_id`),
  KEY `idx_t_space_topic` (`tenant_id`,`topic`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflow_kafka_trigger
-- ----------------------------
DROP TABLE IF EXISTS `workflow_kafka_trigger`;
CREATE TABLE `workflow_kafka_trigger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `trigger_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'KFKA',
  `trigger_id` varchar(128) NOT NULL,
  `datasource_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `expression` text NOT NULL,
  `workflow_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `inputs` text NOT NULL,
  `inputKey` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用工作流的时候作为inputs的一个字段',
  `status` int NOT NULL DEFAULT '0',
  `cuid` bigint NOT NULL DEFAULT '0',
  `muid` bigint NOT NULL,
  `cu_name` varchar(32) NOT NULL DEFAULT '',
  `mu_name` varchar(32) NOT NULL DEFAULT '',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_id` (`trigger_id`),
  KEY `idx_dsid` (`datasource_id`),
  KEY `idx_tenantid` (`tenant_id`,`cuid`,`ctime`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


ALTER TABLE `workflow_run` ADD COLUMN `trigger_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `workflow_scheduling_id`;

UPDATE `workflow_run` set `trigger_id` = `workflow_scheduling_id`;

ALTER TABLE `workflow_scheduling` ADD COLUMN `trigger_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `tenant_id`;
ALTER TABLE `workflow_scheduling` ADD COLUMN `trigger_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SCHD' AFTER `trigger_id`;

UPDATE `workflow_scheduling` set `trigger_id` = `workflow_scheduling_id`;

ALTER TABLE `workflow_scheduling` DROP INDEX `idx_workflow_scheduling_id`;
ALTER TABLE `bella_workflow_junit`.`workflow_scheduling` ADD UNIQUE INDEX `idx_trigger_id`(`trigger_id` ASC) USING BTREE;

SET FOREIGN_KEY_CHECKS = 1;
