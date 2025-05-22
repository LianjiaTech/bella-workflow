alter table `tenant`
    add column `openapi_key` varchar(64) NOT NULL DEFAULT '' after `tenant_name`;
