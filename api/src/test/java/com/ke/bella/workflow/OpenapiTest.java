package com.ke.bella.workflow;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ke.bella.workflow.tool.ApiTool;
import com.ke.bella.workflow.utils.OpenapiUtil;

public class OpenapiTest {

    @Test
    public void testParseOpenAPI2isNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> OpenapiUtil.parseOpenapiToToolBundle(
                new String(Files.readAllBytes(Paths.get("src/test/resources/openapi2.0_schema.json")))));
    }

    @Test
    public void testParseOpenAPi3notNull() {
        AtomicReference<List<ApiTool.ToolBundle>> toolBundles = new AtomicReference<>();
        Assertions.assertDoesNotThrow(() -> toolBundles.set(OpenapiUtil.parseOpenapiToToolBundle(
                new String(Files.readAllBytes(Paths.get("src/test/resources/openapi3.0_schema.json"))))));
        Assertions.assertTrue(toolBundles.get().size() > 0);
    }
}
