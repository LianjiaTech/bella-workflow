package com.ke.bella.workflow.dify;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ke.bella.workflow.AbstractTest;
import com.ke.bella.workflow.api.DifyWorkspacesController;

public class DifyWorkSpacesControllerTest extends AbstractTest {

    @Resource
    DifyWorkspacesController workspacesController;

    @Test
    public void testListToolCollectsSuccess() {
        AtomicReference<List<DifyWorkspacesController.DifyApiToolProvider>> providers = new AtomicReference<>();
        Assertions.assertDoesNotThrow(() -> providers.set(workspacesController.apiTools()));
        Assertions.assertTrue(providers.get().size() > 0);
    }
}
