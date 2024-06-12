
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for instance
-- ----------------------------
DROP TABLE IF EXISTS `instance`;
CREATE TABLE `instance` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ip` varchar(64) NOT NULL,
  `port` int NOT NULL,
  `status` int NOT NULL DEFAULT '0',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ip_port` (`ip`,`port`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for tenant
-- ----------------------------
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tenant_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `cuid` bigint NOT NULL DEFAULT '0',
  `cu_name` varchar(32) NOT NULL DEFAULT '',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `muid` bigint NOT NULL DEFAULT '0',
  `mu_name` varchar(32) NOT NULL DEFAULT '',
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of tenant
-- ----------------------------
BEGIN;
INSERT INTO `tenant` (`id`, `tenant_id`, `tenant_name`, `parent_id`, `cuid`, `cu_name`, `ctime`, `muid`, `mu_name`, `mtime`) VALUES (1, '04633c4f-8638-43a3-a02e-af23c29f821f', 'test', '', 0, 'string', '2024-05-22 14:38:45', 0, 'string', '2024-05-22 14:38:45');
COMMIT;

-- ----------------------------
-- Table structure for workflow
-- ----------------------------
DROP TABLE IF EXISTS `workflow`;
CREATE TABLE `workflow` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'workflow配置自增主键',
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户id',
  `workflow_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(255) NOT NULL DEFAULT '',
  `desc` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `graph` text NOT NULL COMMENT '工作流DAG配置',
  `version` bigint unsigned NOT NULL DEFAULT '0' COMMENT '工作流版本，0: draft, >0 正式版时间戳',
  `cuid` bigint NOT NULL DEFAULT '0',
  `cu_name` varchar(32) NOT NULL DEFAULT '',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `muid` bigint NOT NULL DEFAULT '0',
  `mu_name` varchar(32) NOT NULL DEFAULT '',
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tenant_id` (`tenant_id`,`workflow_id`,`version`) USING BTREE,
  KEY `idx_cuid_time` (`cuid`,`ctime`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ----------------------------
-- Table structure for workflow_node_run
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_run`;
CREATE TABLE `workflow_node_run` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `workflow_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `workflow_run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `node_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `inputs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `outputs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `process_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `notify_data` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `actived_target_handles` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `elapsed_time` bigint unsigned NOT NULL DEFAULT '0',
  `cuid` bigint NOT NULL DEFAULT '0',
  `cu_name` varchar(32) NOT NULL DEFAULT '',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `muid` bigint NOT NULL DEFAULT '0',
  `mu_name` varchar(32) NOT NULL DEFAULT '',
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_workflow_runid` (`workflow_run_id`,`node_id`),
  KEY `idx_workflow_id` (`workflow_id`,`ctime`,`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1282 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ----------------------------
-- Table structure for workflow_run
-- ----------------------------
DROP TABLE IF EXISTS `workflow_run`;
CREATE TABLE `workflow_run` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `workflow_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `workflow_version` bigint NOT NULL,
  `workflow_run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `workflow_run_sharding_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `trigger_from` varchar(64) NOT NULL DEFAULT '' COMMENT '工作流触发来源：\nDEBUG\nDEBUG_NODE\nAPI\n\n',
  `inputs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `outputs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '最后一个节点的输出',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'init' COMMENT '    \n    INIT=‘init’\n    RUNNING = ''running''\n    SUCCEEDED = ''succeeded''\n    FAILED = ''failed''\n    STOPPED = ''stopped''\n',
  `error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `callback_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `callback_status` int NOT NULL DEFAULT '0',
  `cuid` bigint NOT NULL DEFAULT '0',
  `cu_name` varchar(32) NOT NULL DEFAULT '',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `muid` bigint NOT NULL DEFAULT '0',
  `mu_name` varchar(32) NOT NULL DEFAULT '',
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `response_mode` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_workflow_run_id` (`workflow_run_id`),
  KEY `idx_workflow_id` (`workflow_id`,`ctime`,`status`),
  KEY `idx_tenant_id` (`tenant_id`,`ctime`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ----------------------------
-- Table structure for workflow_run_sharding
-- ----------------------------
DROP TABLE IF EXISTS `workflow_run_sharding`;
CREATE TABLE `workflow_run_sharding` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '分表的标识，\n对应的表+’_’+key即是实际读写的表',
  `key_time` datetime NOT NULL,
  `last_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '分表的记录数量',
  `max_count` bigint unsigned NOT NULL DEFAULT '200000000' COMMENT '分表的最大记录数\n如果count>max_count， 创建新表',
  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cu_name` varchar(64) NOT NULL DEFAULT '',
  `cuid` bigint NOT NULL DEFAULT '0',
  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `mu_name` varchar(255) NOT NULL DEFAULT '',
  `muid` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_key` (`key`),
  KEY `idx_last_key` (`last_key`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of workflow_run_sharding
-- ----------------------------
BEGIN;
INSERT INTO `workflow_run_sharding` (`id`, `key`, `key_time`, `last_key`, `count`, `max_count`, `ctime`, `cu_name`, `cuid`, `mtime`, `mu_name`, `muid`) VALUES (1, '', '2024-05-27 16:27:40', 'NO', 42, 200000000, '2024-05-27 16:25:40', '', 0, '2024-05-29 21:08:49', '', 0);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
