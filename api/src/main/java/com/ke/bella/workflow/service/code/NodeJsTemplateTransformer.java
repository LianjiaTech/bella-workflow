package com.ke.bella.workflow.service.code;

public class NodeJsTemplateTransformer implements TemplateTransformer {

    @Override
    public String getRunnerScript() {
        String scriptTemplate = "\n// declare main function" +
                "\n%s" +
                "\n\n// decode and prepare input object" +
                "\nvar inputs_obj = JSON.parse(Buffer.from('%s', 'base64').toString('utf-8'))" +
                "\n\n// execute main function" +
                "\nvar output_obj = main(inputs_obj)" +
                "\n\n// convert output to json and print" +
                "\nvar output_json = JSON.stringify(output_obj)" +
                "\nvar result = `<<RESULT>>${output_json}<<RESULT>>`" +
                "\nconsole.log(result)" +
                "\n";
        return String.format(scriptTemplate, CODE_PLACEHOLDER, INPUTS_PLACEHOLDER);
    }
}
