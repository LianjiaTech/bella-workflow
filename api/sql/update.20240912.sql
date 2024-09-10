ALTER TABLE `workflow_run` ADD COLUMN `flash_mode` int NOT NULL DEFAULT '0' COMMENT '极速模式' AFTER `elapsed_time`;
ALTER TABLE `workflow_run` ADD COLUMN `stateful` tinyint(1) NOT NULL DEFAULT 0 AFTER `span_lev`;

CREATE TABLE `workflow_as_api` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'workflow配置自增主键',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '绑定的域名',
  `path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '绑定的api路径',
  `hash` varchar(128) NOT NULL COMMENT 'host+path的hash',
  `operation_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '生成openapi schema的时候使用的operationId',
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户id',
  `workflow_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作流id',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `desc` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `version` bigint NOT NULL DEFAULT '-1' COMMENT '工作流版本，0: draft, >0 正式版时间戳',
  `status` int NOT NULL DEFAULT '0',
  `cuid` bigint NOT NULL DEFAULT '0',
  `cu_name` varchar(32) NOT NULL DEFAULT '',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `muid` bigint NOT NULL DEFAULT '0',
  `mu_name` varchar(32) NOT NULL DEFAULT '',
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_host_path` (`hash`) USING BTREE,
  KEY `idx_host` (`host`) USING BTREE,
  KEY `idx_wfid` (`tenant_id`,`workflow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;