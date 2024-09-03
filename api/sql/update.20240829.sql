SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE `wecom_group_member` ADD COLUMN `robot_outer_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '机器人外部id' AFTER `robot_id`;

ALTER TABLE `workflow_kafka_trigger` ADD COLUMN `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `trigger_id`;

ALTER TABLE `workflow_kafka_trigger` ADD COLUMN `desc` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `name`;

ALTER TABLE `workflow_kafka_trigger` MODIFY COLUMN `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `id`;

ALTER TABLE `workflow_kafka_trigger` MODIFY COLUMN `inputKey` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'event' COMMENT '调用工作流的时候作为inputs的一个字段' AFTER `inputs`;

ALTER TABLE `workflow_kafka_trigger` ADD INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE;

ALTER TABLE `workflow_scheduling` DROP INDEX `idx_status_trigger_next_time`;

ALTER TABLE `workflow_scheduling` ADD COLUMN `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `trigger_type`;

ALTER TABLE `workflow_scheduling` ADD COLUMN `desc` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `name`;

ALTER TABLE `workflow_scheduling` ADD COLUMN `running_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'init' COMMENT '调度任务状态；\ninit:待执行\npending:已有线程在处理,等待提交workflow_run\nrunning:workflow_run进行中\nfinished:已完成\nerror:出现异常\n:canceled:取消' AFTER `inputs`;

ALTER TABLE `workflow_scheduling` MODIFY COLUMN `trigger_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `tenant_id`;

UPDATE `workflow_scheduling` set `running_status` = `status`;
UPDATE `workflow_scheduling` set `status` = '0';
ALTER TABLE `workflow_scheduling` MODIFY COLUMN `status` int NOT NULL DEFAULT 0 AFTER `running_status`;

ALTER TABLE `workflow_scheduling` ADD INDEX `idx_status_trigger_next_time`(`trigger_next_time` ASC, `running_status` ASC, `status` ASC) USING BTREE;

ALTER TABLE `workflow_scheduling` ADD INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE;

ALTER TABLE `workflow_webot_trigger` ADD COLUMN `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `trigger_id`;

ALTER TABLE `workflow_webot_trigger` ADD COLUMN `desc` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `name`;

ALTER TABLE `workflow_webot_trigger` ADD INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE;

ALTER TABLE `workflow_aggregate` ADD COLUMN `status` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '状态（0:正常, -1:已删除）' AFTER `latest_publish_version`;

ALTER TABLE `workflow_run` add column `thread_id` varchar(64) not null default '' comment 'thread_id' ;

SET FOREIGN_KEY_CHECKS=1;
