-- bella和test
insert into tenant (tenant_id, tenant_name, openapi_key, cuid, cu_name, muid, mu_name)
values ('test', 'test', 'api_key', userId, 'userName', userId, 'userName');

update tenant
set openapi_key = 'api_key'
where tenant_id in ('TENT-f396b9ed-df0b-4bd5-80b1-d668ce244f94', 'test');

-- 绘听
update tenant
set openapi_key = 'PPR47yWyKXIxRFLRRiB8XrhgrXOA78BL'
where tenant_id = 'TENT-d815410c-f9db-459e-b4ab-67a52d8e63ce';
