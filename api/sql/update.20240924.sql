ALTER TABLE `wecom_group_info`
    ADD COLUMN `thread_id` varchar(64) NOT NULL DEFAULT '' COMMENT '会话id' AFTER `chat_id`;
