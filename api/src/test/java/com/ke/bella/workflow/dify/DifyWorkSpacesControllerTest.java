package com.ke.bella.workflow.dify;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;

import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.db.BellaContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ke.bella.workflow.AbstractTest;
import com.ke.bella.workflow.api.DifyWorkspacesController;

public class DifyWorkSpacesControllerTest extends AbstractTest {

    @Resource
    DifyWorkspacesController workspacesController;

	@BeforeEach
	public void initBellaContext() {
		BellaContext.setOperator(
			Operator.builder().userId(userIdL).tenantId("04633c4f-8638-43a3-a02e-af23c29f821f").userName("mock").build());
		BellaContext.setApiKey("8O1uNhMF5k9O8tkmmjLo1rhiPe7bbzX8");
	}

    @Test
    public void testListToolCollectsSuccess() {
        AtomicReference<List<DifyWorkspacesController.DifyApiToolProvider>> providers = new AtomicReference<>();
        Assertions.assertDoesNotThrow(() -> providers.set(workspacesController.apiTools()));
        Assertions.assertTrue(providers.get().size() > 0);
    }
}
