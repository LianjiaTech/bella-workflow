
alter table `workflow_node_run`
    add column `node_run_id` varchar(128) NOT NULL DEFAULT '' after `workflow_run_id`;