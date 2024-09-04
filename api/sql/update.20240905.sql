ALTER TABLE `workflow_run` ADD COLUMN `metadata` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `response_mode`;

ALTER TABLE `workflow_run` ADD COLUMN `thread_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'thread_id' AFTER `metadata`;