package com.ke.bella.workflow.api.model;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterRule {
    private String name;
    private String useTemplate;
    private I18nObject label;
    private String type;
    private I18nObject help;
    private boolean required;
    private Object defaultValue;
    private Double min;
    private Double max;
    private Integer precision;
    private List<String> options = new ArrayList<>();

    public static List<ParameterRule> getRules(int maxToken, boolean supportJsonObject, boolean supportJsonSchema) {
        List<ParameterRule> rules = new ArrayList<>();
        rules.add(PARAMETER_RULE_TEMPLATE.get(ParameterName.TEMPERATURE));
        rules.add(PARAMETER_RULE_TEMPLATE.get(ParameterName.TOP_P));
        rules.add(PARAMETER_RULE_TEMPLATE.get(ParameterName.PRESENCE_PENALTY));
        rules.add(PARAMETER_RULE_TEMPLATE.get(ParameterName.FREQUENCY_PENALTY));
        if(maxToken > 0) {
            rules.add(maxTokenRule(maxToken));
        } else {
            rules.add(PARAMETER_RULE_TEMPLATE.get(ParameterName.MAX_TOKENS));
        }
        if(supportJsonSchema) {
            rules.add(PARAMETER_RULE_TEMPLATE.get(ParameterName.JSON_SCHEMA_RESPONSE_FORMAT));
            rules.add(PARAMETER_RULE_TEMPLATE.get(ParameterName.JSON_SCHEMA));
        } else if(supportJsonObject) {
            rules.add(PARAMETER_RULE_TEMPLATE.get(ParameterName.RESPONSE_FORMAT));
        }
        return rules;
    }

    public enum ParameterName {
        TEMPERATURE,
        TOP_P,
        PRESENCE_PENALTY,
        FREQUENCY_PENALTY,
        MAX_TOKENS,
        RESPONSE_FORMAT,
        JSON_SCHEMA_RESPONSE_FORMAT,
        JSON_SCHEMA
    }

    public static Map<ParameterName, ParameterRule> PARAMETER_RULE_TEMPLATE = new HashMap<>();

    static {
        PARAMETER_RULE_TEMPLATE.put(ParameterName.TEMPERATURE, new ParameterRule(
                "temperature",
                null,
                new I18nObject("Temperature", "温度"),
                ParameterType.FLOAT.getValue(),
                new I18nObject(
                        "Controls randomness. Lower temperature results in less random completions. As the temperature approaches zero, the model will become deterministic and repetitive. Higher temperature results in more random completions.",
                        "温度控制随机性。较低的温度会导致较少的随机完成。随着温度接近零，模型将变得确定性和重复性。较高的温度会导致更多的随机完成。"
                ),
                false,
                0.0,
                0.0,
                1.0,
                2,
                Collections.emptyList()
        ));

        PARAMETER_RULE_TEMPLATE.put(ParameterName.TOP_P, new ParameterRule(
                "top_p",
                null,
                new I18nObject("Top P", "Top P"),
                ParameterType.FLOAT.getValue(),
                new I18nObject(
                        "Controls diversity via nucleus sampling: 0.5 means half of all likelihood-weighted options are considered.",
                        "通过核心采样控制多样性：0.5表示考虑了一半的所有可能性加权选项。"
                ),
                false,
                1.0,
                0.0,
                1.0,
                2,
                Collections.emptyList()
        ));

        PARAMETER_RULE_TEMPLATE.put(ParameterName.PRESENCE_PENALTY, new ParameterRule(
                "presence_penalty",
                null,
                new I18nObject("Presence Penalty", "存在惩罚"),
                ParameterType.FLOAT.getValue(),
                new I18nObject(
                        "Applies a penalty to the log-probability of tokens already in the text.",
                        "对文本中已有的标记的对数概率施加惩罚。"
                ),
                false,
                0.0,
                0.0,
                1.0,
                2,
                Collections.emptyList()
        ));

        PARAMETER_RULE_TEMPLATE.put(ParameterName.FREQUENCY_PENALTY, new ParameterRule(
                "frequency_penalty",
                null,
                new I18nObject("Frequency Penalty", "频率惩罚"),
                ParameterType.FLOAT.getValue(),
                new I18nObject(
                        "Applies a penalty to the log-probability of tokens that appear in the text.",
                        "对文本中出现的标记的对数概率施加惩罚。"
                ),
                false,
                0.0,
                0.0,
                1.0,
                2,
                Collections.emptyList()
        ));

        PARAMETER_RULE_TEMPLATE.put(ParameterName.MAX_TOKENS, new ParameterRule(
                "max_tokens",
                null,
                new I18nObject("Max Tokens", "最大标记"),
                ParameterType.INT.getValue(),
                new I18nObject(
                        "Specifies the upper limit on the length of generated results. If the generated results are truncated, you can increase this parameter.",
                        "指定生成结果长度的上限。如果生成结果截断，可以调大该参数。"
                ),
                false,
                64,
                1.0,
                4096.0,
                0,
                Collections.emptyList()
        ));

        PARAMETER_RULE_TEMPLATE.put(ParameterName.RESPONSE_FORMAT, new ParameterRule(
                "response_format",
                null,
                new I18nObject("Response Format", "回复格式"),
                ParameterType.STRING.getValue(),
                new I18nObject(
                        "Set a response format, ensure the output from llm is a valid code block as possible, such as JSON, XML, etc.",
                        "设置一个返回格式，确保llm的输出尽可能是有效的代码块，如JSON"
                ),
                false,
                Optional.empty(),
                null,
                null,
                null,
                ImmutableList.of("text", "json_object")
        ));

        PARAMETER_RULE_TEMPLATE.put(ParameterName.JSON_SCHEMA_RESPONSE_FORMAT, new ParameterRule(
                "response_format",
                null,
                new I18nObject("Response Format", "回复格式"),
                ParameterType.STRING.getValue(),
                new I18nObject(
                        "Set a response format, ensure the output from llm is a valid code block as possible, such as JSON, XML, etc.",
                        "设置一个返回格式，确保llm的输出尽可能是有效的代码块，如JSON"
                ),
                false,
                Optional.empty(),
                null,
                null,
                null,
                ImmutableList.of("text", "json_object", "json_schema")
        ));

        PARAMETER_RULE_TEMPLATE.put(ParameterName.JSON_SCHEMA, new ParameterRule(
                "json_schema",
                null,
                new I18nObject("JSON Schema", "JSON Schema"),
                ParameterType.TEXT.getValue(),
                new I18nObject(
                        "Set a response json schema will ensure LLM to adhere it.",
                        "设置返回的json schema，llm将按照它返回"
                ),
                false,
                null,
                null,
                null,
                null,
                Collections.emptyList()
        ));
    }

    private static ParameterRule maxTokenRule(int maxToken) {
       return new ParameterRule(
                "max_tokens",
                null,
                new I18nObject("Max Tokens", "最大标记"),
                ParameterType.INT.getValue(),
                new I18nObject(
                        "Specifies the upper limit on the length of generated results. If the generated results are truncated, you can increase this parameter.",
                        "指定生成结果长度的上限。如果生成结果截断，可以调大该参数。"
                ),
                false,
                64,
                1.0,
                maxToken * 1.0,
                0,
                Collections.emptyList()
        );
    }
}
