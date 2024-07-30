alter table `tenant`
    add column `openapi_key` varchar(64) NOT NULL DEFAULT '' after `tenant_name`;
update `tenant`
set `openapi_key`= 'api_key'
where tenant_id = '04633c4f-8638-43a3-a02e-af23c29f821f'
