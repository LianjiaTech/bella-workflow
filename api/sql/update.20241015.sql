ALTER TABLE `workflow_aggregate`
    ADD COLUMN default_publish_version BIGINT NOT NULL DEFAULT -1  comment '默认生效版本号 -1 使用最新 ' AFTER `latest_publish_version`;
