package com.ke.bella.workflow.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;
import com.ke.bella.workflow.JsonUtils;

@RestController
@RequestMapping("/console/api/workspaces")
public class DifyWorkspacesController {

    @Value("${bella.llm.models}")
    private String llmModels;

    @GetMapping("/current/models/model-types/{model_type}")
    public Object llmModel(@PathVariable("model_type") String modelType) {
        // 默认返回c4ai-command-r-plus
        return ImmutableMap.of("data", JsonUtils.fromJson(
                llmModels, List.class));
    }
}
