-- ----------------------------
-- Table structure for tenants
-- ----------------------------
DROP TABLE IF EXISTS `tenants`;
CREATE TABLE `tenants`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `tenant_id`   varchar(32)  NOT NULL          DEFAULT '',
    `tenant_name` varchar(128) NOT NULL          DEFAULT '',
    `cuid`        bigint       NOT NULL          DEFAULT '0',
    `cu_name`     varchar(32)  NOT NULL          DEFAULT '',
    `ctime`       datetime     NOT NULL NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `muid`        bigint       NOT NULL          DEFAULT '0',
    `mu_name`     varchar(32)  NOT NULL          DEFAULT '',
    `mtime`       datetime     NOT NULL NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflow_node_runs
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_runs`;
CREATE TABLE `workflow_node_runs`
(
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT,
    `tenant_id`       varchar(32)  NOT NULL          DEFAULT '',
    `workflow_id`     varchar(128) NOT NULL          DEFAULT '',
    `workflow_run_id` varchar(128) NOT NULL          DEFAULT '',
    `node_id`         varchar(128) NOT NULL          DEFAULT '',
    `node_type`       varchar(64)  NOT NULL          DEFAULT '',
    `title`           varchar(128) NOT NULL          DEFAULT '',
    `inputs`          text,
    `outputs`         text,
    `error`           text,
    `process_data`    text,
    `status`          varchar(32)  NOT NULL          DEFAULT '',
    `elapsed_time`    bigint unsigned NOT NULL DEFAULT '0',
    `cuid`            bigint       NOT NULL          DEFAULT '0',
    `cu_name`         varchar(32)  NOT NULL          DEFAULT '',
    `ctime`           datetime     NOT NULL NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `muid`            bigint       NOT NULL          DEFAULT '0',
    `mu_name`         varchar(32)  NOT NULL          DEFAULT '',
    `mtime`           datetime     NOT NULL NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY               `idx_workflow_runid` (`workflow_run_id`,`node_id`),
    KEY               `idx_workflow_id` (`workflow_id`,`ctime`,`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflow_runs
-- ----------------------------
DROP TABLE IF EXISTS `workflow_runs`;
CREATE TABLE `workflow_runs`
(
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT,
    `tenant_id`       varchar(32)  NOT NULL          DEFAULT '',
    `workflow_id`     varchar(128) NOT NULL          DEFAULT '',
    `workflow_run_id` varchar(128) NOT NULL          DEFAULT '',
    `trigger_from`    varchar(64)  NOT NULL          DEFAULT '' COMMENT '工作流触发来源：\nDEBUG\nDEBUG_NODE\nAPI\n\n',
    `inputs`          text,
    `outputs`         text COMMENT '最后一个节点的输出',
    `status`          varchar(32)  NOT NULL COMMENT 'RUNNING = ''running''\n    SUCCEEDED = ''succeeded''\n    FAILED = ''failed''\n    STOPPED = ''stopped''\n',
    `error`           text,
    `cuid`            bigint       NOT NULL          DEFAULT '0',
    `cu_name`         varchar(32)  NOT NULL          DEFAULT '',
    `ctime`           datetime     NOT NULL NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `muid`            bigint       NOT NULL          DEFAULT '0',
    `mu_name`         varchar(32)  NOT NULL          DEFAULT '',
    `mtime`           datetime     NOT NULL NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_workflow_run_id` (`workflow_run_id`),
    KEY               `idx_workflow_id` (`workflow_id`,`ctime`,`status`),
    KEY               `idx_tenant_id` (`tenant_id`,`ctime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflows
-- ----------------------------
DROP TABLE IF EXISTS `workflows`;
CREATE TABLE `workflows`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'workflow配置自增主键',
    `tenant_id`   varchar(32)  NOT NULL COMMENT '租户id',
    `workflow_id` varchar(128) NOT NULL          DEFAULT '',
    `graph`       text         NOT NULL COMMENT '工作流DAG配置',
    `version`     bigint unsigned NOT NULL DEFAULT '0' COMMENT '工作流版本，0: draft, >0 正式版时间戳',
    `cuid`        bigint       NOT NULL          DEFAULT '0',
    `cu_name`     varchar(32)  NOT NULL          DEFAULT '',
    `ctime`       datetime     NOT NULL NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `muid`        bigint       NOT NULL          DEFAULT '0',
    `mu_name`     varchar(32)  NOT NULL          DEFAULT '',
    `mtime`       datetime     NOT NULL NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_workflow_id` (`workflow_id`),
    KEY           `idx_cuid_time` (`cuid`,`ctime`),
    KEY           `idx_tenant_id` (`tenant_id`,`cuid`,`mtime`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
