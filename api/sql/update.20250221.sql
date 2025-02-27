alter table `workflow`
    add release_description varchar(1024) default '' not null after version;
alter table `workflow_aggregate`
    add release_description varchar(1024) default '' not null after version;
