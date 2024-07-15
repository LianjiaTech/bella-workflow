
alter table `workflow`
    add column `mode` varchar(64) NOT NULL DEFAULT 'workflow' after `title`;

alter table `workflow_aggregate`
    add column `mode` varchar(64) NOT NULL DEFAULT 'workflow' after `title`;
    
alter table `workflow_node_run`
    add column `query` varchar(4096) NOT NULL DEFAULT '' after `trigger_from`;