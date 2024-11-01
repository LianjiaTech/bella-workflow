alter table `workflow_aggregate`
    add column `space_code` varchar(64) not null default '' after workflow_id;

update `workflow_aggregate`
set `space_code` = cuid;
