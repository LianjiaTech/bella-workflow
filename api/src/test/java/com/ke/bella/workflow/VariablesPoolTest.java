package com.ke.bella.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.utils.JsonUtils;

public class VariablesPoolTest {

    @Test
    public void testFormatAllJsonTypes() {
        HashMap<String, Object> input = new HashMap<>();
        HashMap<String, Object> values = new HashMap<>();
        values.put("string", "test");
        values.put("number", 1);
        values.put("boolean", true);
        values.put("object", Collections.singletonMap("test", "test"));
        values.put("array", Collections.singletonList("test"));
        values.put("null", null);
        input.put("1718608984746", values);
        String content = "{\"string\":\"{{#1718608984746.string#}}\",\"number\":{{#1718608984746.number#}},\"boolean\":{{#1718608984746.boolean#}},\"object\":{{#1718608984746.object#}},\"array\":{{#1718608984746.array#}}}";
        String afterFormat = Variables.format(content, "{{#", "#}}", input);
        Assertions.assertDoesNotThrow(() -> JsonUtils.fromJson(afterFormat, Map.class));
        Map mapAfterFormat = JsonUtils.fromJson(afterFormat, Map.class);
        Assertions.assertTrue(mapAfterFormat.get("string") instanceof String);
        Assertions.assertTrue(mapAfterFormat.get("number") instanceof Integer);
        Assertions.assertTrue(mapAfterFormat.get("boolean") instanceof Boolean);
        Assertions.assertTrue(mapAfterFormat.get("object") instanceof Map);
        Assertions.assertTrue(mapAfterFormat.get("array") instanceof List);
        Assertions.assertFalse(mapAfterFormat.containsKey("null"));
    }

    @Test
    public void testFormatWithJsonEscape() {
        String origin = "{\"model\":\"{{#env.model#}}\",\"messages\":[{\"role\":\"user\",\"content\":\"{{#1742811059936.output#}}\"}],\"max_new_tokens\":2000,\"do_sample\":false,\"temperature\":0.01,\"top_p\":0.01,\"repetition_penalty\":1.0,\"stream\":false}";

        Map pool = JsonUtils.fromJson(
                "{\"1742811059936\":{\"output\":\"**角色**: 时间格式转化专家\\n**任务**: 把给定的时间描述转化为一种唯一表示的中间时间状态。当前任务是对[年]、[季度]、[月]、[周]、[日]的时间组合维度表达进行pattern转换。最小的时间单位为天，转化为一个时间点，其他情况下都是一个时间范围。\\n\\n**注意事项**:\\n - 今年为2025年，本月是3月。\\n\\n**例子**:\\n\\n```\\n输入:前天\\n 输出:-2d\\n\\n输入:前几天\\n 输出:between -3d and 0d\\n\\n输入:今天\\n 输出:0d\\n\\n输入:昨天\\n 输出:-1d\\n\\n输入:大前天\\n 输出:-3d\\n\\n输入: 本月1号或者这个月一号\\n 输出: 0ms\\n\\n输入: 上月3号或者上个月三号\\n 输出: -1m3D\\n\\n输入: 上月第三天\\n 输出: -1m2d\\n\\n输入: 本季度第二个月\\n 输出: between 0q1ms and 0q1me\\n\\n输入: 今年Q2或者今年第二季度\\n 输出: between 0y2Qs and 0y2Qe\\n\\n输入: Q2或者二季度或者第二季度\\n 输出: between 2Qs and 2Qe\\n\\n输入: 3月(和当前时间月份一致时)\\n 输出: between 3Ms and 0d\\n\\n输入: 2023年8月1日到现在\\n 输出: between 2023Y8Y1D and 0d\\n\\n输入: 9月22号或者9.22号或者0922或者9/22\\n 输出: 2023Y9M22D\\n\\n输入: 2025年\\n 输出: between 2025Ys and 0d\\n\\n输入: 2020年\\n 输出: between 2020Ys and 2020Yd\\n\\n输入: 近两个月\\n 输出: between -1ms and 0d\\n\\n输入: 10月32号\\n 输出: 不合理的时间表达\\n```\\n\\n**规则**:\\n\\n- **Pattern输出**:\\n - **格式1(时间点)**: [时间数字1][单位1][时间数字2][单位2],...,[时间数字k][单位:日]\\n - **格式2(时间区间)**: between [起始时间点] and [结束时间点]\\n\\n- 如果描述的时间不存在，如9月31号、10月45号等，则输出“不合理的时间表达”。\\n\\n- 's'和'e'可作为[年]、[季度]、[月]、[周]单位的后缀，且仅在时间点不包含具体日期时使用。's'表示起始日期，'e'表示结束日期。例子: ys表示年初第一天，ye表示年末最后一天。\\n\\n- **年维度**的可选用{Y、y}，其用法规则如下：\\n - **[用法1]Y**: 表示年时间点的单位\\n - 例子: query: 2023年 pattern: between 2023Ys and 2023Ye\\n - **[用法2]y**: 表示年时间点相对时间单位（从0开始计数）\\n - 例子: query: 去年 pattern: between -1ys and -1ye\\n\\n- **季度维度**的可选用{Q、q}，**月维度**的可选用{M、m}，**周维度**的可选用{W、w}，**日维度**的可选用{d、D}，并且[季度]、[月]、[周]和[日]大写字母和小写字母单位为区分，其用法规则如下：\\n- **[用法1]**: **Q、M、W、D(大写字母单位)**: 表示时间点的单位\\n - 例子: query: 2024年2季度 pattern: between 2024Y2Qs and 2024Y2Qe\\n- **[用法2]**: **Q、M(大写字母单位)**: 永远表示基准时间为年的相对时间的单位（从1开始计数）\\n - 例子1: query: 第2季度 pattern: between 2Qs and 2Qe\\n - 例子2: query: 2024年第2月 pattern: between 2024Y2Ms and 2024Y2Me\\n- **[用法3]**: **W、D(大写字母单位)**: 表示基准时间为前置单位的相对时间的单位（从1开始计数），且表示周一到周日的完整周\\n - 例子1: query: 本年第2周 pattern: between 0y2Ws and 0y2We\\n - 例子2: query: 本月第2周 pattern: between 0m2Ws and 0m2We\\n- **[用法4]**: **Q、M、W、D(大写字母单位)**: 表示末尾时间倒叙的相对时间的单位（从1开始计数）\\n - 例子1: query: 今年后2季度 pattern: between 0y-2Qs and 0y-1Qe\\n - 例子2: query: 2024年第二季度后两月 pattern: between 2024Y2Q-2Ms and 2024Y2Q-1Me\\n- **[用法5]**: **m(小写字母单位)**: 表示基准时间为季度的前置单位的相对时间的单位（从0开始计数）\\n - 例子1: query: 二季度第二月 pattern: between 2Q1ms and 2Q1me\\n - 例子2: query: 2014年第二月 pattern: between 2024Y2Ms and 2024Y2Me (采用大写M作为单位)\\n- **[用法6]**: **w(小写字母单位)**: 表示query无前置时间点叙述的不完整时间间隔的相对时间的单位（从0开始计数）\\n - 例子1: query: 近两周 pattern: between -ws and 0d\\n - 例子2: query: 2014年的前两周 pattern: between 2024Y1Ws and 2024Y2We (采用大写W作为单位)\\n- **[用法7]**: **y、q、m、d(小写字母单位)**: 表示相对时间的单位（从0开始计数）\\n - 例子1: query: 近两月 pattern: between -ms and 0d\\n- 优先参考给定的例子，如果有完全相同的输入则直接输出例子中的输出，如果有相似的输入，参考例子中的输出。\\n输入:大大前天\\n输出:\\n\\n答：\",\"docs\":[{\"chunk_content\":\"-2d\",\"abstract_content\":\"-2d\",\"id\":\"1884011355\",\"_score\":0.7402344942092896,\"title_content\":\"前天\"},{\"chunk_content\":\"between -3d and 0d\",\"abstract_content\":\"between -3d and 0d\",\"id\":\"283115015\",\"_score\":0.7356124520301819,\"title_content\":\"前几天\"},{\"chunk_content\":\"0d\",\"abstract_content\":\"0d\",\"id\":\"862530946\",\"_score\":0.7262519598007202,\"title_content\":\"今天\"},{\"chunk_content\":\"-1d\",\"abstract_content\":\"-1d\",\"id\":\"156723439\",\"_score\":0.6929193735122681,\"title_content\":\"昨天\"},{\"chunk_content\":\"-3d\",\"abstract_content\":\"-3d\",\"id\":\"-1054836574\",\"_score\":0.6387494206428528,\"title_content\":\"大前天\"}],\"month\":3,\"year\":2025,\"day\":25,\"query\":\"大大前天\"},\"env\":{\"openapi_auth\":\"qaekDD2hBoZE4ArZZlOQ9fYTQ74Qc8mq\",\"kb_token\":\"KnowledgeBaseRoute\",\"model\":\"ali-qwen2-14b-timeprocess-v1-chat-20240819\",\"openapi_url\":\"http://example.com\",\"kb_url\":\"http://example.com/api/time_few_shot_kg_new/retrieval\"}}",
                new TypeReference<HashMap>() {
                });
        String format = Variables.formatJson(origin, pool);
        Assertions.assertDoesNotThrow(() -> JsonUtils.fromJson(format, Object.class));
        Object json = JsonUtils.fromJson(format, Object.class);
        Assertions.assertNotNull(json);
    }

}
