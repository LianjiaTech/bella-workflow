package com.ke.bella.workflow.service.code;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Python3TemplateTransformer implements TemplateTransformer {

    public static Set<String> STANDARD_PACKAGES = Sets.newHashSet(
            "base64",
            "binascii",
            "collections",
            "datetime",
            "functools",
            "hashlib",
            "hmac",
            "itertools",
            "json",
            "math",
            "operator",
            "os",
            "random",
            "re",
            "string",
            "sys",
            "time",
            "traceback",
            "uuid",
            "requests",
            "httpx",
            "jinja2");

    @Override
    public Set<String> getStandardPackages() {
        return STANDARD_PACKAGES;
    }

    @Override
    public String getRunnerScript() {
        String scriptTemplate = "\n# declare main function" +
                "\n%s" +
                "\n\nimport json" +
                "\nfrom base64 import b64decode" +
                "\n\n# decode and prepare input dict" +
                "\ninputs_obj = json.loads(b64decode('%s').decode('utf-8'))" +
                "\n\n# execute main function" +
                "\noutput_obj = main(**inputs_obj)" +
                "\n\n# convert output to json and print" +
                "\noutput_json = json.dumps(output_obj, indent=4)" +
                "\nresult = f'''<<RESULT>>{output_json}<<RESULT>>'''" +
                "\nprint(result)" +
                "\n";
        return String.format(scriptTemplate, CODE_PLACEHOLDER, INPUTS_PLACEHOLDER);
    }

    @Override
    public List<CodeExecutor.CodeDependency> getDefaultAvailablePackages() {
        return CodeExecutor.getDependencies(getLanguage());
    }

    @Override
    public CodeExecutor.CodeLanguage getLanguage() {
        return CodeExecutor.CodeLanguage.python3;
    }

    @Override
    public String getDefaultCode() {
        return "\ndef main(arg1: int, arg2: int) -> dict:\n" +
                "    return {\n" +
                "        \"result\": arg1 + arg2,\n" +
                "    }\n";
    }

    @Override
    public String getPreloadScript() {
        StringBuilder script = new StringBuilder();
        for (String packageName : STANDARD_PACKAGES) {
            script.append("import ").append(packageName).append("\n");
        }
        return script.toString();
    }
}
