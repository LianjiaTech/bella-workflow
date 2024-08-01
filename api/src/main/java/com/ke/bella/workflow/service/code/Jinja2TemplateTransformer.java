package com.ke.bella.workflow.service.code;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class Jinja2TemplateTransformer implements TemplateTransformer {
    @Override
    public Set<String> getStandardPackages() {
        return Stream.concat(Python3TemplateTransformer.STANDARD_PACKAGES.stream(), Sets.newHashSet("jinja2").stream()).collect(Collectors.toSet());
    }

    @Override
    public String getRunnerScript() {
        String scriptTemplate = "\n# declare main function" +
                "\ndef main(**inputs):" +
                "\n    import jinja2" +
                "\n    template = jinja2.Template('''%s''')" +
                "\n    return template.render(**inputs)" +
                "\n\nimport json" +
                "\nfrom base64 import b64decode" +
                "\n\n# decode and prepare input dict" +
                "\ninputs_obj = json.loads(b64decode('%s').decode('utf-8'))" +
                "\n\n# execute main function" +
                "\noutput = main(**inputs_obj)" +
                "\n\n# convert output and print" +
                "\nresult = f'''<<RESULT>>{output}<<RESULT>>'''" +
                "\nprint(result)" +
                "\n\n";
        return String.format(scriptTemplate, CODE_PLACEHOLDER, INPUTS_PLACEHOLDER);
    }

    public String getPreloadScript() {
        return "\nimport jinja2" +
                "\nfrom base64 import b64decode" +
                "\n\ndef _jinja2_preload_():" +
                "\n    # prepare jinja2 environment, load template and render before to avoid sandbox issue" +
                "\n    template = jinja2.Template('{{s}}')" +
                "\n    template.render(s='a')" +
                "\n\nif __name__ == '__main__':" +
                "\n    _jinja2_preload_()" +
                "\n\n";
    }

    @Override
    public Map<String, Object> transformResponse(String response) {
        return ImmutableMap.of("result", extractResultStrFromResponse(response));
    }
}
