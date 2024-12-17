ALTER TABLE `workflow_run` 
    ADD COLUMN `context` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' AFTER `stateful`;