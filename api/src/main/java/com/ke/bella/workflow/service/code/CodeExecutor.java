package com.ke.bella.workflow.service.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class CodeExecutor {

    private static final String X_API_KEY = "bella-workflow-sandbox";

    private static final String CODE_SANDBOX_URL = Configs.OPEN_API_BASE + "sandbox/run";

    private static final String DEPENDENCIES_SANDBOX_URL = Configs.OPEN_API_BASE + "sandbox/dependencies";

    private static final Map<String, String> CODE_LANGUAGE_TO_RUNNING_LANGUAGE = ImmutableMap.of(
            CodeLanguage.javascript.name(), "nodejs",
            CodeLanguage.jinja2.name(), CodeLanguage.python3.name(),
            CodeLanguage.python3.name(), CodeLanguage.python3.name());

    private static final Map<String, TemplateTransformer> transformers = ImmutableMap.of(
            CodeLanguage.python3.name(), new Python3TemplateTransformer(),
            CodeLanguage.jinja2.name(), new Jinja2TemplateTransformer(),
            CodeLanguage.javascript.name(), new NodeJsTemplateTransformer(),
            CodeLanguage.groovy.name(), new GroovyTemplateTransformer());

    public static Object execute(CodeLanguage language, String code, Map<String, Object> inputs, List<CodeDependency> dependencies) {
        TemplateTransformer transformer = Optional.ofNullable(transformers.get(language.name()))
                .orElseThrow(() -> new IllegalArgumentException("unsupported code language"));

        if(language == CodeLanguage.groovy) {
            Object result = GroovySandbox.execute(code, inputs);
            if(result instanceof Map) {
                return result;
            } else if(result instanceof NodeRunResult) {
                return result;
            } else {
                throw new IllegalArgumentException("返回值类型必须是Map");
            }
        } else {
            TemplateTransformer.RunParams runner = transformer.transformRunParams(code, inputs, dependencies);
            String resp = executeCode(language, runner.getRunnerScript(), runner.getPreloadScript(),
                    CollectionUtils.isEmpty(runner.getPackages()) ? null : runner.getPackages());

            return transformer.transformResponse(resp);
        }
    }

    public static Map<String, Object> getDefaultConfig(CodeLanguage language) {
        return Optional.ofNullable(transformers.get(language.name())).orElseThrow(() -> new IllegalArgumentException("unsupported code language"))
                .defaultConfig();
    }

    private static String executeCode(CodeLanguage language, String code, String preload, List<CodeDependency> dependencies) {
        CodeRunOp op = CodeRunOp.builder()
                .language(CODE_LANGUAGE_TO_RUNNING_LANGUAGE.get(language.name()))
                .code(code)
                .preload(preload)
                .dependencies(dependencies).build();

        SandBoxResp<SandBoxResp.CodeRunResult> sandBoxResp = HttpUtils.postJson(ImmutableMap.of("X-Api-Key", X_API_KEY), CODE_SANDBOX_URL,
                JsonUtils.toJson(op),
                new TypeReference<SandBoxResp<SandBoxResp.CodeRunResult>>() {
                });

        if(sandBoxResp.getCode() != 0) {
            throw new IllegalStateException(String.format("Code Node run failed: %s", sandBoxResp.getMessage()));
        } else if(!StringUtils.isEmpty(sandBoxResp.getData().getError())) {
            throw new IllegalStateException(String.format("Code Node run failed: %s", sandBoxResp.getData().getError()));
        }

        return sandBoxResp.getData().getStdout();
    }

    public static List<CodeDependency> getDependencies(CodeLanguage language) {
        HashMap<String, String> params = new HashMap<>();
        params.put("language", CODE_LANGUAGE_TO_RUNNING_LANGUAGE.get(language.name()));

        SandBoxResp<SandBoxResp.DependenciesResult> sandBoxResp = HttpUtils.get(ImmutableMap.of("X-Api-Key", X_API_KEY),
                DEPENDENCIES_SANDBOX_URL,
                params,
                new TypeReference<SandBoxResp<SandBoxResp.DependenciesResult>>() {
                });

        if(sandBoxResp.getCode() != 0) {
            throw new IllegalStateException(String.format("Code Node run failed: %s", sandBoxResp.getMessage()));
        } else if(Objects.isNull(sandBoxResp.getData()) || CollectionUtils.isEmpty(sandBoxResp.getData().getDependencies())) {
            return Collections.emptyList();
        }
        return sandBoxResp.getData().getDependencies()
                .stream().filter(dep -> !transformers.get(language.name()).getStandardPackages().contains(dep.getName()))
                .collect(Collectors.toList());
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class CodeRunOp {
        private String language;
        private String code;
        private String preload;
        @Builder.Default
        @JsonProperty("enable_network")
        private Boolean enableNetwork = true;
        private List<CodeDependency> dependencies;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class SandBoxResp<T> {
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @lombok.Data
        public static class CodeRunResult {
            private String stdout;
            private String error;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @lombok.Data
        public static class DependenciesResult {
            @Builder.Default
            private List<CodeDependency> dependencies = new ArrayList<>();
        }

        private int code;
        private String message;
        private T data;
    }

    @Getter
    public enum CodeLanguage {
        python3,
        jinja2,
        javascript,
        groovy;

        public static CodeLanguage of(String code) {
            return Stream.of(CodeLanguage.values())
                    .filter(i -> i.name().equals(code))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("invalid code language %s", code)));
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeDependency {
        private String name;
        private String version = "";

        public CodeDependency(String name) {
            this.name = name;
        }
    }
}
