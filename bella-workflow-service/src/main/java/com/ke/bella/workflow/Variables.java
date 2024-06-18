package com.ke.bella.workflow;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hubspot.jinjava.Jinjava;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.StringLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public class Variables {
    private static final PebbleEngine engine = new PebbleEngine.Builder()
            .loader(new StringLoader())
            .build();

    @SuppressWarnings("rawtypes")
    public static Object getValue(Map pool, String selectors) {
        String[] selector = selectors.split("\\.");
        return getValue(pool, Arrays.asList(selector));
    }

    @SuppressWarnings("rawtypes")
    public static Object getValue(Map pool, List<String> selector) {
        if(selector.isEmpty()) {
            return null;
        }
        try {
            Object result = pool;
            for (String key : selector) {
                if(result instanceof Map) {
                    result = ((Map) result).get(key);
                } else {
                    result = result.getClass().getDeclaredField(key).get(result);
                }
                if(result == null) {
                    return null;
                }
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void putValue(Map<String, Map> pool, List<String> selector, Object value) {
        if(selector.isEmpty()) {
            return;
        }

        Map parent = pool;
        Map result = parent;
        for (int i = 0; i < selector.size() - 1; i++) {
            String key = selector.get(i);
            result = (Map) result.get(key);
            if(result == null) {
                result = new LinkedHashMap<>();
                parent.put(key, result);
            } else if(!(result instanceof Map)) {
                throw new IllegalArgumentException("变量不合法");
            }
            parent = result;
        }
        result.put(selector.get(selector.size() - 1), value);
    }

    @SuppressWarnings("rawtypes")
    public static String getValueAsString(Map pool, String selectors) {
        Object v = getValue(pool, selectors);
        if(v == null || v instanceof String) {
            return (String) v;
        }
        return JsonUtils.toJson(v);
    }

    @SuppressWarnings("rawtypes")
    public static String format(String text, Map pool) {
        return format(text, "{{#", "#}}", pool);
    }

    @SuppressWarnings("rawtypes")
    public static String format(String text, String openToken, String closeToken, Map map) {
        if(map == null || map.size() == 0) {
            return text;
        }
        if(text == null || text.isEmpty()) {
            return "";
        }
        char[] src = text.toCharArray();
        int offset = 0;
        // search open token
        int start = text.indexOf(openToken, offset);
        if(start == -1) {
            return text;
        }
        final StringBuilder builder = new StringBuilder();
        StringBuilder expression = null;
        while (start > -1) {
            if(start > 0 && src[start - 1] == '\\') {
                // this open token is escaped. remove the backslash and
                // continue.
                builder.append(src, offset, start - offset - 1).append(openToken);
                offset = start + openToken.length();
            } else {
                // found open token. let's search close token.
                if(expression == null) {
                    expression = new StringBuilder();
                } else {
                    expression.setLength(0);
                }
                builder.append(src, offset, start - offset);
                offset = start + openToken.length();
                int end = text.indexOf(closeToken, offset);
                while (end > -1) {
                    if(end > offset && src[end - 1] == '\\') {
                        // this close token is escaped. remove the backslash and
                        // continue.
                        expression.append(src, offset, end - offset - 1).append(closeToken);
                        offset = end + closeToken.length();
                        end = text.indexOf(closeToken, offset);
                    } else {
                        expression.append(src, offset, end - offset);
                        offset = end + closeToken.length();
                        break;
                    }
                }
                if(end == -1) {
                    // close token was not found.
                    builder.append(src, start, src.length - start);
                    offset = src.length;
                } else {
                    String key = text.substring(start + openToken.length(), end);
                    Object value = getValue(map, key);
                    builder.append(value == null ? "" : JsonUtils.toJson(value).replaceAll("\\\\", "/"));
                    offset = end + closeToken.length();
                }
            }
            start = text.indexOf(openToken, offset);
        }
        if(offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }

    public static String render(String tmpl, Map<String, Object> context) {
        String text = tmpl;
        try {
            PebbleTemplate t = engine.getTemplate(tmpl);

            Writer writer = new StringWriter();
            t.evaluate(writer, context);

            text = writer.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("模版渲染失败: " + e.getMessage(), e);
        }

        return text;
    }

    public static String renderJinjia(String tmpl, Map<String, Object> context) {
        Jinjava jinjava = new Jinjava();
        return jinjava.render(tmpl, context);
    }

}
