package com.ke.bella.workflow.service.code;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
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

    private static final String CODE_SANDBOX_URL = Configs.API_BASE + "sandbox/run";

    private static final Map<String, String> CODE_LANGUAGE_TO_RUNNING_LANGUAGE = ImmutableMap.of(
            CodeLanguage.javascript.name(), "nodejs",
            CodeLanguage.jinja2.name(), CodeLanguage.python3.name(),
            CodeLanguage.python3.name(), CodeLanguage.python3.name());

    private static final Map<String, TemplateTransformer> transformers = ImmutableMap.of(
            CodeLanguage.python3.name(), new Python3TemplateTransformer(),
            CodeLanguage.jinja2.name(), new Jinja2TemplateTransformer(),
            CodeLanguage.javascript.name(), new NodeJsTemplateTransformer());

    public static Map<String, Object> execute(CodeLanguage language, String code, Map<String, Object> inputs, List<CodeDependency> dependencies) {
        TemplateTransformer transformer = Optional.ofNullable(transformers.get(language.name()))
                .orElseThrow(() -> new IllegalArgumentException("unsupported code language"));

        TemplateTransformer.RunParams runner = transformer.transformRunParams(code, inputs, dependencies);

        String resp = executeCode(language, runner.getRunnerScript(), runner.getPreloadScript(),
                CollectionUtils.isEmpty(runner.getPackages()) ? null : runner.getPackages());

        return transformer.transformResponse(resp);
    }

    private static String executeCode(CodeLanguage language, String code, String preload, List<CodeDependency> dependencies) {
        CodeRunOp op = CodeRunOp.builder()
                .language(CODE_LANGUAGE_TO_RUNNING_LANGUAGE.get(language.name()))
                .code(code)
                .preload(preload)
                .dependencies(dependencies).build();

        CodeRunResp codeRunResp = HttpUtils.postJson(ImmutableMap.of("X-Api-Key", X_API_KEY), CODE_SANDBOX_URL, JsonUtils.toJson(op),
                new TypeReference<CodeRunResp>() {
                });

        if(codeRunResp.getCode() != 0) {
            throw new IllegalStateException(String.format("Code Node run failed: %s", codeRunResp.getMessage()));
        } else if(!StringUtils.isEmpty(codeRunResp.getData().getError())) {
            throw new IllegalStateException(String.format("Code Node run failed: %s", codeRunResp.getData().getError()));
        }

        return codeRunResp.getData().getStdout();
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
    public static class CodeRunResp {
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @lombok.Data
        public static class Data {
            private String stdout;
            private String error;
        }

        private int code;
        private String message;
        private Data data;
    }

    @Getter
    public enum CodeLanguage {
        python3,
        jinja2,
        javascript;

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
