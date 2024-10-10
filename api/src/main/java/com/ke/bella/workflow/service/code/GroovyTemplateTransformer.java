package com.ke.bella.workflow.service.code;

public class GroovyTemplateTransformer implements TemplateTransformer {

    @Override
    public String getRunnerScript() {
        return CODE_PLACEHOLDER;
    }

    @Override
    public CodeExecutor.CodeLanguage getLanguage() {
        return CodeExecutor.CodeLanguage.groovy;
    }

    @Override
    public String getDefaultCode() {
        return "return [result: 'John Doe']";
    }
}
