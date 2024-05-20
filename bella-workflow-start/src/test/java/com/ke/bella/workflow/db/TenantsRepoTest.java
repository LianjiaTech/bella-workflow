package com.ke.bella.workflow.db;

import com.ke.bella.workflow.AbstractTest;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.List;

public class TenantsRepoTest extends AbstractTest {

	@Resource
	private DSLContext db;

	@Test
	public void testInsertTenantsRepo() {
        // 插入一个mock对象 Tables.TENANT
        TenantDB tenantsDB = new TenantDB();
		tenantsDB.setTenantName("test");
		tenantsDB.setTenantId("test");
        List<TenantDB> beforeInsert = db.select().from(Tables.TENANT).where(Tables.TENANT.TENANT_NAME.eq("test")).fetchInto(TenantDB.class);
		Assertions.assertThat(beforeInsert.size()).isLessThan(1);
        db.insertInto(Tables.TENANT)
                .set(Tables.TENANT.TENANT_ID, tenantsDB.getTenantId())
                .set(Tables.TENANT.TENANT_NAME, tenantsDB.getTenantName())
			.execute();
        List<TenantDB> afterInsert = db.select().from(Tables.TENANT).where(Tables.TENANT.TENANT_NAME.eq("test")).fetchInto(TenantDB.class);
		Assertions.assertThat(afterInsert.size()).isGreaterThanOrEqualTo(1);
	}
}
