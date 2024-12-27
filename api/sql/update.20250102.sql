alter table `workflow_kafka_trigger` add column `expression_type` varchar(16) not null default '' comment '表达式脚本语言类型' after `expression`;
alter table `workflow_webot_trigger` add column `expression_type` varchar(16) not null default '' comment '表达式脚本语言类型' after `expression`;

update workflow_kafka_trigger set `expression_type` = 'Aviator' where expression is not null and expression != '';
update workflow_webot_trigger set `expression_type` = 'Aviator' where expression is not null and expression != '';
