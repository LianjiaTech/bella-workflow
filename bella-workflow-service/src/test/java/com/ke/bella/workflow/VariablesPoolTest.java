package com.ke.bella.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VariablesPoolTest {

    @Test
    public void testFormatAllJsonTypes() {
        HashMap<String, Object> input = new HashMap<>();
        HashMap<String, Object> values = new HashMap<>();
        values.put("string", "test");
        values.put("number", 1);
        values.put("boolean", true);
        values.put("object", Collections.singletonMap("test", "test"));
        values.put("array", Collections.singletonList("test"));
        values.put("null", null);
        input.put("1718608984746", values);
        String content = "{\"string\":\"{{#1718608984746.string#}}\",\"number\":{{#1718608984746.number#}},\"boolean\":{{#1718608984746.boolean#}},\"object\":{{#1718608984746.object#}},\"array\":{{#1718608984746.array#}}}";
        String afterFormat = Variables.format(content, "{{#", "#}}", input);
        Assertions.assertDoesNotThrow(() -> JsonUtils.fromJson(afterFormat, Map.class));
        Map mapAfterFormat = JsonUtils.fromJson(afterFormat, Map.class);
        Assertions.assertTrue(mapAfterFormat.get("string") instanceof String);
        Assertions.assertTrue(mapAfterFormat.get("number") instanceof Integer);
        Assertions.assertTrue(mapAfterFormat.get("boolean") instanceof Boolean);
        Assertions.assertTrue(mapAfterFormat.get("object") instanceof Map);
        Assertions.assertTrue(mapAfterFormat.get("array") instanceof List);
        Assertions.assertFalse(mapAfterFormat.containsKey("null"));
    }

}
