DROP TABLE IF EXISTS `workflow_template`;
CREATE TABLE `workflow_template`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT,
    `tenant_id`   varchar(64)                             not null comment '租户id',
    `space_code`  varchar(64)   default ''                not null comment '',
    `template_id` varchar(128)                            not null comment '',
    `workflow_id` varchar(128)                            not null,
    `version`     bigint unsigned default 0 not null,
    `title`       varchar(255)  default ''                not null,
    `mode`        varchar(64)   default 'workflow'        not null,
    `desc`        varchar(1024) default ''                not null,
    `tags`        text                                    not null comment '标签',
    `status`      int           default 0                 not null,
    `copies`      bigint        default 0                 not null comment '复制次数',
    `cuid`        bigint        default 0                 not null,
    `cu_name`     varchar(32)   default ''                not null,
    `ctime`       datetime      default CURRENT_TIMESTAMP not null,
    `muid`        bigint        default 0                 not null,
    `mu_name`     varchar(32)   default ''                not null,
    `mtime`       datetime      default CURRENT_TIMESTAMP not null,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_tenant_id_space_code_template_id` (`tenant_id`, `space_code`, `template_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
