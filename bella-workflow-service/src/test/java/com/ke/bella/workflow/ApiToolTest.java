package com.ke.bella.workflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ke.bella.workflow.tool.ApiTool;
import com.ke.bella.workflow.utils.OpenapiUtil;
import com.theokanning.openai.completion.chat.UserMessage;

public class ApiToolTest {

    @Test
    public void whenInputValidApiToolPostSuccess() throws IOException {
        Map openapi = JsonUtils.fromJson(new String(Files.readAllBytes(Paths.get("src/test/resources/openapi_post_with_ref_schema.json"))),
                Map.class);
        ApiTool.ToolBundle toolBundle = OpenapiUtil.extractToolBundleFromOpenapi(openapi, "createChatCompletion");
        ApiTool apiTool = new ApiTool(toolBundle, new ApiTool.Credentials("Authorization", "Bearer 2Z7cobwsMMShaTKhA108F5wGWoSxmtUt"));
        ImmutableMap<String, Object> inputs = ImmutableMap.of("model", "gpt-4", "messages", Lists.newArrayList(new UserMessage("你好")));
        String execute = apiTool.execute(inputs);
        Assertions.assertNotNull(execute);
    }

    @Test
    public void whenLostRequiredParamsToolPostError() throws IOException {
        Map openapi = JsonUtils.fromJson(new String(Files.readAllBytes(Paths.get("src/test/resources/openapi_post_with_ref_schema.json"))),
                Map.class);
        ApiTool.ToolBundle toolBundle = OpenapiUtil.extractToolBundleFromOpenapi(openapi, "createChatCompletion");
        ApiTool apiTool = new ApiTool(toolBundle, new ApiTool.Credentials("Authorization", "Bearer 2Z7cobwsMMShaTKhA108F5wGWoSxmtUt"));
        ImmutableMap<String, Object> inputs = ImmutableMap.of("messages", Lists.newArrayList(new UserMessage("你好")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> apiTool.execute(inputs));
    }

    @Test
    public void whenInputValidPathVariableThatSuccess() throws IOException {
        Map openapi = JsonUtils.fromJson(new String(Files.readAllBytes(Paths.get("src/test/resources/openapi_get_with_path_variable_schema.json"))),
                Map.class);
        ApiTool.ToolBundle toolBundle = OpenapiUtil.extractToolBundleFromOpenapi(openapi, "modelServiceHealthCheck");
        ApiTool apiTool = new ApiTool(toolBundle, null);
        ImmutableMap<String, Object> inputs = ImmutableMap.of("model_service_name", "ali-qwen15-72b-zfy-v1-chat-20240314", "lianjia_cookie",
                "lianjia_ssid=1787e6f7-06e9-4b59-9519-edc11524e633; lianjia_uuid=12eccb7b-0906-4113-a6d1-7a458c48801e");
        Assertions.assertDoesNotThrow(() -> apiTool.execute(inputs));
    }

    @Test
    public void whenGet404ThatThrowException() throws IOException {
        Map openapi = JsonUtils.fromJson(new String(Files.readAllBytes(Paths.get("src/test/resources/openapi_get_with_path_variable_schema.json"))),
                Map.class);
        ApiTool.ToolBundle toolBundle = OpenapiUtil.extractToolBundleFromOpenapi(openapi, "modelServiceHealthCheck");
        ApiTool apiTool = new ApiTool(toolBundle, null);
        ImmutableMap<String, Object> inputs = ImmutableMap.of("model_service_name", "not_found_model_service", "lianjia_cookie",
                "lianjia_ssid=1787e6f7-06e9-4b59-9519-edc11524e633; lianjia_uuid=12eccb7b-0906-4113-a6d1-7a458c48801e");
        Assertions.assertThrows(IllegalStateException.class, () -> apiTool.execute(inputs));
    }

}
