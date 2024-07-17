
alter table `workflow`
    add column `mode` varchar(64) NOT NULL DEFAULT 'workflow' after `title`;

alter table `workflow_aggregate`
    add column `mode` varchar(64) NOT NULL DEFAULT 'workflow' after `title`;
    
alter table `workflow_run`
    add column `query` TEXT after `trigger_from`;
alter table `workflow_run`
    add column `files` TEXT after `query`;