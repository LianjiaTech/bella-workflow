package com.ke.bella.workflow.service.code;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.service.code.CodeExecutor.CodeDependency;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public interface TemplateTransformer {
    String CODE_PLACEHOLDER = "{{code}}";
    String INPUTS_PLACEHOLDER = "{{inputs}}";
    String RESULT_TAG = "<<RESULT>>";

    default Set<String> getStandardPackages() {
        return Collections.emptySet();
    }

    default RunParams transformRunParams(String code, Map<String, Object> inputs,
            List<CodeDependency> dependencies) {
        String runnerScript = assembleRunnerScript(code, inputs);
        String preloadScript = getPreloadScript();

        List<CodeDependency> packages = Optional.ofNullable(dependencies).orElse(new ArrayList<>());
        Set<String> standardPackages = getStandardPackages();
        packages.addAll(standardPackages.stream()
                .filter(packageName -> packages.stream().noneMatch(dep -> dep.getName().equals(packageName)))
                .map(CodeDependency::new)
                .collect(Collectors.toList()));

        return RunParams.builder().runnerScript(runnerScript).preloadScript(preloadScript).packages(packages).build();
    }

    default String extractResultStrFromResponse(String response) {
        Pattern pattern = Pattern.compile(RESULT_TAG + "(.*)" + RESULT_TAG, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if(!matcher.find()) {
            throw new IllegalStateException(String.format("failed parse result from %s", response));
        }
        return matcher.group(1);
    }

    default Map<String, Object> transformResponse(String response) {
        String resultStr = extractResultStrFromResponse(response);
        return JsonUtils.fromJson(resultStr, new TypeReference<Map<String, Object>>() {
        });
    }

    default String serializeInputs(Map<String, Object> inputs) {
        String inputsJsonStr = JsonUtils.toJson(inputs);
        return Base64.getEncoder().encodeToString(inputsJsonStr.getBytes(StandardCharsets.UTF_8));
    }

    default String assembleRunnerScript(String code, Map<String, Object> inputs) {
        String script = getRunnerScript();
        script = script.replace(CODE_PLACEHOLDER, code);
        String inputsStr = serializeInputs(inputs);
        script = script.replace(INPUTS_PLACEHOLDER, inputsStr);
        return script;
    }

    default String getPreloadScript() {
        return "";
    }

    String getRunnerScript();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class RunParams {
        private String runnerScript;
        private String preloadScript;
        private List<CodeDependency> packages;
    }
}
