alter table workflow_run_sharding modify column max_count bigint unsigned not null default 20000000 comment '分表的最大记录数\n如果count>max_count， 创建新表';
