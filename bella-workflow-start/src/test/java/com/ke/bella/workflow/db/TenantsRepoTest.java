package com.ke.bella.workflow.db;

import com.ke.bella.workflow.AbstractTest;
import com.ke.bella.workflow.db.tables.pojos.TenantsDB;
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
		//插入一个mock对象 Tables.TENANTS
		TenantsDB tenantsDB = new TenantsDB();
		tenantsDB.setTenantName("test");
		tenantsDB.setTenantId("test");
		List<TenantsDB> beforeInsert = db.select().from(Tables.TENANTS).where(Tables.TENANTS.TENANT_NAME.eq("test")).fetchInto(TenantsDB.class);
		Assertions.assertThat(beforeInsert.size()).isLessThan(1);
		db.insertInto(Tables.TENANTS)
			.set(Tables.TENANTS.TENANT_ID, tenantsDB.getTenantId())
			.set(Tables.TENANTS.TENANT_NAME, tenantsDB.getTenantName())
			.execute();
		List<TenantsDB> afterInsert = db.select().from(Tables.TENANTS).where(Tables.TENANTS.TENANT_NAME.eq("test")).fetchInto(TenantsDB.class);
		Assertions.assertThat(afterInsert.size()).isGreaterThanOrEqualTo(1);
	}
}
