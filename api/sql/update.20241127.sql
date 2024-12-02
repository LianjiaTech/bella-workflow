DROP TABLE IF EXISTS `redis_datasource`;
CREATE TABLE `redis_datasource` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户ID',
                                  `datasource_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                  `space_code` varchar(255) NOT NULL,
                                  `host` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机',
                                  `port` int unsigned NOT NULL COMMENT '端口',
                                  `db` int unsigned NOT NULL DEFAULT '0' COMMENT '默认数据库',
                                  `user` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '用户名',
                                  `password` varchar(255) NOT NULL DEFAULT '' COMMENT '密码',
                                  `status` int NOT NULL DEFAULT '0' COMMENT '数据源状态\n-1: 无效\n0: 生效',
                                  `cuid` bigint NOT NULL DEFAULT '0',
                                  `muid` bigint NOT NULL DEFAULT '0',
                                  `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `cu_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
                                  `mu_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `idx_id` (`datasource_id`),
                                  KEY `idx_t_space_topic` (`tenant_id`,`space_code`(128),`host`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
