package com.ke.bella.workflow.node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.OpenAiUtils;
import com.theokanning.openai.assistants.run.Function;
import com.theokanning.openai.assistants.run.ToolChoice;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatTool;
import com.theokanning.openai.completion.chat.ChatToolCall;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ToolMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.function.FunctionDefinition;
import com.theokanning.openai.service.OpenAiService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParameterExtractorNode extends BaseNode<ParameterExtractorNode.Data> {

    private static final String FUNCTION_CALLING_EXTRACTOR_NAME = "extract_parameters";

    private static final String FUNCTION_CALLING_EXTRACTOR_DESCRIPTION = "Extract parameters from the natural language text";

    private static final String CHAT_GENERATE_JSON_USER_MESSAGE_TEMPLATE = "### Structure\n" +
            "Here is the structure of the JSON object, you should always follow the structure.\n" +
            "<structure>\n" +
            "%s\n" +
            "</structure>\n" +
            "\n" +
            "### Text to be converted to JSON\n" +
            "Inside <text></text> XML tags, there is a text that you should convert to a JSON object.\n" +
            "<text>\n" +
            "%s\n" +
            "</text>\n";

    private static final String FUNCTION_CALLING_EXTRACTOR_SYSTEM_PROMPT = "You are a helpful assistant tasked with extracting structured information based on specific criteria provided. Follow the guidelines below to ensure consistency and accuracy.\n"
            +
            "### Task\n" +
            "Always call the `%s` function with the correct parameters. Ensure that the information extraction is contextual and aligns with the provided criteria.\n"
            +
            "### Instructions:\n" +
            "Some additional information is provided below. Always adhere to these instructions as closely as possible:\n" +
            "<instruction>\n" +
            "%s\n" +
            "</instruction>\n" +
            "Steps:\n" +
            "1. Extract the relevant information based on the criteria given, output multiple values if there is multiple relevant information that match the criteria in the given text. \n"
            +
            "2. Generate a well-formatted output using the defined functions and arguments.\n" +
            "3. Use the `extract_parameter` function to create structured outputs with appropriate parameters.\n" +
            "4. Do not include any XML tags in your output.\n" +
            "### Example\n" +
            "To illustrate, if the task involves extracting a user's name and their request, your function call might look like this: Ensure your output follows a similar structure to examples.\n"
            +
            "### Final Output\n" +
            "Produce well-formatted function calls in json without XML tags, as shown in the example.\n";

    private static final String FUNCTION_CALLING_EXTRACTOR_USER_TEMPLATE = "extract structured information from context inside <context></context> XML tags by calling the function %s with the correct parameters with structure inside <structure></structure> XML tags.\\n<context>\\n%s\n"
            + "</context>\\n\n"
            + "<structure>\\n%s\n"
            + "</structure>\\n";

    public ParameterExtractorNode(WorkflowSchema.Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
    }

    @Override
    @SuppressWarnings("all")
    protected WorkflowRunState.NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map<String, Object> inputs = new HashMap<>();
        Map<String, Object> processData = new HashMap<>();
        Map<String, Object> outputs = new HashMap<>();
        try {
            String query = Variables.getValueAsString(context.getState().getVariablePool(), data.getQuery());
            if(Objects.isNull(query)) {
                throw new IllegalArgumentException("Input variable content not found or is empty");
            }
            // 1. validate parameters
            data.getParameters().forEach(Data.ParameterConfig::validate);

            // 2. fill inputs map
            inputs.put("query", query);
            inputs.put("parameters", data.getParameters());
            inputs.put("instruction", data.getInstruction());

            // 3. integrate system and user instruct
            String systemPrompt = String.format(FUNCTION_CALLING_EXTRACTOR_SYSTEM_PROMPT, FUNCTION_CALLING_EXTRACTOR_NAME,
                    Variables.format(data.getInstruction(), context.getState().getVariablePool()));

            // fixme：check model support tool_calls or not
            // 4. build params by different reasoning mode
            List<ChatMessage> chatMessages = null;
            ChatTool extraParamsTool = null;
            if(Data.ReasoningMode.functionCall.getValue().equals(data.getReasoningMode())) {
                chatMessages = generateFunctionCallMessages(query, systemPrompt);

                extraParamsTool = new ChatTool(FunctionDefinition.builder()
                        .name(FUNCTION_CALLING_EXTRACTOR_NAME)
                        .description(FUNCTION_CALLING_EXTRACTOR_DESCRIPTION)
                        .parametersDefinition(data.getParameterJsonSchema())
                        .build());
            } else {
                chatMessages = generatePromptEngineeringPrompt(query, systemPrompt);
            }

            if(data.getVision().enabledWithFiles()) {
                chatMessages = appendVisionMessages(chatMessages, data.getVision(), context.getState().getVariablePool());
            }

            processData.put("model_mode", "chat");
            processData.put("prompts", chatMessages);
            processData.put("function", extraParamsTool);

            // 5. invoke llm
            ChatCompletionResult chatResultCompletion = invokeLlm(chatMessages, extraParamsTool);
            ChatCompletionChoice chatCompletionChoice = chatResultCompletion.getChoices().get(0);
            AssistantMessage chatResult = chatCompletionChoice.getMessage();
            ChatToolCall toolCall = Optional.ofNullable(chatResult.getToolCalls()).map(toolCalls -> toolCalls.get(0)).orElse(null);
            String llmText = chatResult.getContent();
            processData.put("tool_call", toolCall);
            processData.put("llm_text", llmText);

            // 6. handle invoke result
            Map<String, Object> result = handleInvokeResult(chatResult, data.getReasoningMode());

            outputs.put("__is_success", 1);
            outputs.put("__reason", null);
            outputs.put("__usage", chatResultCompletion.getUsage());
            outputs.put("__finish_reason", chatCompletionChoice.getFinishReason());
            outputs.putAll(result);

            return WorkflowRunState.NodeRunResult.builder()
                    .status(WorkflowRunState.NodeRunResult.Status.succeeded)
                    .inputs(inputs)
                    .outputs(outputs)
                    .processData(processData).build();
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
            outputs.put("__is_success", 0);
            outputs.put("__reason", e.getMessage());
            return WorkflowRunState.NodeRunResult.builder()
                    .status(WorkflowRunState.NodeRunResult.Status.failed)
                    .inputs(inputs)
                    .outputs(outputs)
                    .error(e)
                    .processData(processData).build();
        }
    }

    private Map<String, Object> handleInvokeResult(AssistantMessage chatResult, String reasoningMode) {
        Map<String, Object> result = null;
        if(Data.ReasoningMode.functionCall.getValue().equals(reasoningMode)) {
            // tool call failed
            if(CollectionUtils.isEmpty(chatResult.getToolCalls()) || Objects.isNull(chatResult.getToolCalls().get(0).getFunction())) {
                throw new IllegalArgumentException("tool call failed, model return invalid tool call result");
            }
            result = Optional.ofNullable(chatResult.getToolCalls().get(0).getFunction().getArguments())
                    .map(args -> JsonUtils.fromJson(args.toString(), new TypeReference<Map<String, Object>>() {
                    }))
                    .orElse(Collections.emptyMap());
        } else {
            result = extractParamsFromText(chatResult.getContent());
        }

        validateResult(data, result);

        // 兜底逻辑
        result = transformResult(data, result);
        return result;
    }

    private void validateResult(Data data, Map<String, Object> result) {
        if(Objects.isNull(result)) {
            throw new IllegalArgumentException("Invalid extract result");
        }
        for (Data.ParameterConfig parameter : data.parameters) {
            if(parameter.required && !result.containsKey(parameter.name)) {
                throw new IllegalArgumentException("Parameter " + parameter.name + " is required");
            }

            Object value = result.get(parameter.name);
            if(value == null) {
                continue;
            }
            if("select".equals(parameter.type) && parameter.options != null && !parameter.options.contains(value)) {
                throw new IllegalArgumentException("Invalid `select` value for parameter " + parameter.name);
            }

            if("number".equals(parameter.type) && !(value instanceof Number)) {
                throw new IllegalArgumentException("Invalid `number` value for parameter " + parameter.name);
            }

            if("bool".equals(parameter.type) && !(value instanceof Boolean)) {
                throw new IllegalArgumentException("Invalid `bool` value for parameter " + parameter.name);
            }

            if("string".equals(parameter.type) && !(value instanceof String)) {
                throw new IllegalArgumentException("Invalid `string` value for parameter " + parameter.name);
            }

            if(parameter.type.startsWith("array")) {
                if(!(value instanceof List)) {
                    throw new IllegalArgumentException("Invalid `array` value for parameter " + parameter.name);
                }
                String nestedType = parameter.type.substring(6, parameter.type.length() - 1);
                List<?> listValue = (List<?>) value;
                for (Object item : listValue) {
                    switch (nestedType) {
                    case "number":
                        if(!(item instanceof Number)) {
                            throw new IllegalArgumentException("Invalid `array[number]` value for parameter " + parameter.name);
                        }
                        break;
                    case "string":
                        if(!(item instanceof String)) {
                            throw new IllegalArgumentException("Invalid `array[string]` value for parameter " + parameter.name);
                        }
                        break;
                    case "object":
                        if(!(item instanceof Map)) {
                            throw new IllegalArgumentException("Invalid `array[object]` value for parameter " + parameter.name);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown array item type for parameter " + parameter.name);
                    }
                }
            }
        }
    }

    private Map<String, Object> extractParamsFromText(String text) {
        for (int idx = 0; idx < text.length(); idx++) {
            if(text.charAt(idx) == '{' || text.charAt(idx) == '[') {
                String jsonStr = extractJsonStr(text.substring(idx));
                if(jsonStr != null) {
                    try {
                        return JsonUtils.fromJson(jsonStr, new TypeReference<Map<String, Object>>() {
                        });
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        return Collections.emptyMap();
    }

    private String extractJsonStr(String text) {
        Deque<Character> stack = new ArrayDeque<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if(c == '{' || c == '[') {
                stack.push(c);
            } else if(c == '}' || c == ']') {
                if(stack.isEmpty()) {
                    return text.substring(0, i);
                }
                if((c == '}' && stack.peek() == '{') || (c == ']' && stack.peek() == '[')) {
                    stack.pop();
                    if(stack.isEmpty()) {
                        return text.substring(0, i + 1);
                    }
                } else {
                    return text.substring(0, i);
                }
            }
        }
        return null;
    }

    private ChatCompletionResult invokeLlm(List<ChatMessage> chatMessages, ChatTool tool) {
        OpenAiService service = OpenAiUtils.defaultOpenAiService(data.getAuthorization().getToken(), data.getTimeout().getReadSeconds(),
                TimeUnit.SECONDS);
        ChatCompletionRequest chatCompletionRequest = data.getModel().getTemplateCompletionParams();
        chatCompletionRequest.setMessages(chatMessages);
        if(Objects.nonNull(tool)) {
            chatCompletionRequest.setTools(Collections.singletonList(tool));

            FunctionDefinition function = (FunctionDefinition) tool.getFunction();
            ToolChoice toolChoice = new ToolChoice(new Function(function.getName()));
            chatCompletionRequest.setToolChoice(toolChoice);
        }
        chatCompletionRequest.setUser(String.valueOf(BellaContext.getOperator().getUserId()));
        // fixme:
        // 1. 三方包流式请求，严格校验协议；
        // 2. 自研模型响应体"stop"不是"tool_call"导致解析失败，此处先采用非流式请求，由于是tool_call不影响速度
        return service.createChatCompletion(chatCompletionRequest);
    }

    private List<ChatMessage> generatePromptEngineeringPrompt(String query, String systemPrompt) {
        return generateMessages(query, systemPrompt, fewShotsByPrompts());
    }

    private List<ChatMessage> generateFunctionCallMessages(String query, String systemPrompt) {
        return generateMessages(query, systemPrompt, fewShotsByToolCalls());
    }

    private List<ChatMessage> generateMessages(String query, String systemPrompt, List<ChatMessage> fewShotsMessages) {
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(String.format(FUNCTION_CALLING_EXTRACTOR_USER_TEMPLATE, FUNCTION_CALLING_EXTRACTOR_NAME, query,
                JsonUtils.toJson(data.getParameterJsonSchema())));
        List<ChatMessage> result = new ArrayList<>();
        result.add(systemMessage);
        result.addAll(fewShotsMessages);
        result.add(userMessage);
        return result;
    }

    /**
     * few shots about extracting params by "extract_parameters" tool calls
     */
    private static List<ChatMessage> fewShotsByToolCalls() {
        String id1 = UUID.randomUUID().toString();
        UserMessage user1 = new UserMessage("What is the weather today in SF?");
        ObjectNode params1 = JsonNodeFactory.instance.objectNode().put("location", "San Francisco");
        ChatFunctionCall chatFunctionCall = new ChatFunctionCall(FUNCTION_CALLING_EXTRACTOR_NAME, params1);
        ChatToolCall chatToolCall = new ChatToolCall(1, id1, "function", chatFunctionCall);
        AssistantMessage assistantMessage1Before = new AssistantMessage(
                "I need always call the function with the correct parameters. in this case, I need to call the function with the location parameter.",
                "", null, Collections.singletonList(chatToolCall), null, null, null);
        ToolMessage toolMessage1 = new ToolMessage("Great! You have called the function with the correct parameters.", id1);
        AssistantMessage assistantMessage1After = new AssistantMessage("I have extracted the parameters, let\\'s move on.");

        String id2 = UUID.randomUUID().toString();
        UserMessage user2 = new UserMessage("I want to eat some apple pie");
        ObjectNode params2 = JsonNodeFactory.instance.objectNode().put("food", "apple pie");
        AssistantMessage assistantMessage2Before = new AssistantMessage(
                "I need always call the function with the correct parameters. in this case, I need to call the function with the food parameter.", null,
                null, Collections.singletonList(new ChatToolCall(1, id2, "function", new ChatFunctionCall(FUNCTION_CALLING_EXTRACTOR_NAME, params2))),
                null, null, null);
        ToolMessage toolMessage2 = new ToolMessage("Great! You have called the function with the correct parameters.", id2);
        AssistantMessage assistantMessage2After = new AssistantMessage("I have extracted the parameters, let\\'s move on.");

        return Lists.newArrayList(user1, assistantMessage1Before, toolMessage1, assistantMessage1After, user2, assistantMessage2Before, toolMessage2,
                assistantMessage2After);
    }

    /**
     * few shots about extracting params by prompts
     */
    private static List<ChatMessage> fewShotsByPrompts() {
        UserMessage userMessage1 = new UserMessage(String.format(CHAT_GENERATE_JSON_USER_MESSAGE_TEMPLATE,
                "{\"type\":\"object\",\"properties\":{\"location\":{\"type\":\"string\",\"description\":\"The location to get the weather information\",\"required\":true}},\"required\":[\"location\"]}",
                "What is the weather today in SF?"));
        AssistantMessage assistantMessage1 = new AssistantMessage("{\"location\":\"San Francisco\"}");

        UserMessage userMessage2 = new UserMessage(String.format(CHAT_GENERATE_JSON_USER_MESSAGE_TEMPLATE,
                "{\"type\":\"object\",\"properties\":{\"food\":{\"type\":\"string\",\"description\":\"The food to eat\",\"required\":true}},\"required\":[\"food\"]}",
                "I want to eat some apple pie."));
        AssistantMessage assistantMessage2 = new AssistantMessage("{\"result\":\"apple pie\"}");
        return Lists.newArrayList(userMessage1, assistantMessage1, userMessage2, assistantMessage2);
    }

    public Map<String, Object> transformResult(Data data, Map<String, Object> result) {
        Map<String, Object> transformedResult = new HashMap<>();

        for (Data.ParameterConfig parameter : data.getParameters()) {
            if(result.containsKey(parameter.getName())) {
                Object value = result.get(parameter.getName());
                switch (parameter.getType()) {
                case "number":
                    transformedResult.put(parameter.getName(), transformNumber(value));
                    break;
                case "string":
                case "select":
                    if(value instanceof String) {
                        transformedResult.put(parameter.getName(), value);
                    }
                    break;
                default:
                    if(parameter.getType().startsWith("array")) {
                        transformedResult.put(parameter.getName(), transformArray(value, parameter.getType()));
                    }
                    break;
                }
            }

            if(!transformedResult.containsKey(parameter.getName())) {
                transformedResult.put(parameter.getName(), getDefaultValue(parameter.getType()));
            }
        }

        return transformedResult;
    }

    private Object getDefaultValue(String type) {
        switch (type) {
        case "number":
            return 0;
        case "bool":
            return false;
        case "string":
        case "select":
            return "";
        default:
            if(type.startsWith("array")) {
                return new ArrayList<>();
            }
            return null;
        }
    }

    private Object transformNumber(Object value) {
        if(value instanceof Number) {
            return value;
        } else if(value instanceof String) {
            String strValue = (String) value;
            try {
                if(strValue.contains(".")) {
                    return Double.parseDouble(strValue);
                } else {
                    return Integer.parseInt(strValue);
                }
            } catch (NumberFormatException e) {
                // Return null or handle the exception as needed
            }
        }
        return null;
    }

    private Object transformArray(Object value, String type) {
        if(value instanceof List) {
            List<?> listValue = (List<?>) value;
            List<Object> transformedList = new ArrayList<>();
            String nestedType = type.substring(6, type.length() - 1);
            for (Object item : listValue) {
                switch (nestedType) {
                case "number":
                    transformedList.add(transformNumber(item));
                    break;
                case "string":
                    if(item instanceof String) {
                        transformedList.add(item);
                    }
                    break;
                case "object":
                    if(item instanceof Map) {
                        transformedList.add(item);
                    }
                    break;
                default:
                    break;
                }
            }
            return transformedList;
        }
        return new ArrayList<>();
    }

    public static Map<String, Object> defaultConfig(Map<String, Object> filters) {
        return JsonUtils.fromJson(
                "{\"model\":{\"prompt_templates\":{\"completion_model\":{\"conversation_histories_role\":{\"user_prefix\":\"Human\",\"assistant_prefix\":\"Assistant\"},\"stop\":[\"Human:\"]}}}}",
                new TypeReference<Map<String, Object>>() {
                });
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Data extends BaseNode.BaseNodeData {
        private Model model;
        @Builder.Default
        private Vision vision = new Vision();
        private List<String> query;
        private List<ParameterConfig> parameters;
        private String instruction;
        @JsonAlias("reasoning_mode")
        @Builder.Default
        private String reasoningMode = ReasoningMode.functionCall.getValue();
        @Builder.Default
        Timeout timeout = new Timeout();
        @Builder.Default
        Authorization authorization = new Authorization();

        @lombok.Getter
        @lombok.Setter
        public static class Timeout {
            int readSeconds = 600;
        }

        @SuppressWarnings("all")
        public Map getParameterJsonSchema() {
            Map parameters = new HashMap();
            parameters.put("type", "object");
            Map properties = new HashMap();
            List<Object> required = new ArrayList<>();

            for (ParameterConfig parameter : this.parameters) {
                Map parameterSchema = new HashMap();
                parameterSchema.put("description", parameter.description);

                if(parameter.type.equals("string") || parameter.type.equals("select")) {
                    parameterSchema.put("type", "string");
                } else if(parameter.type.startsWith("array")) {
                    parameterSchema.put("type", "array");
                    String nestedType = parameter.type.substring(6, parameter.type.length() - 1);
                    Map items = new HashMap();
                    items.put("type", nestedType);
                    parameterSchema.put("items", items);
                } else {
                    parameterSchema.put("type", parameter.type);
                }

                if(parameter.type.equals("select")) {
                    List<String> enumArray = new ArrayList<>(parameter.options);
                    parameterSchema.put("enum", enumArray);
                }

                properties.put(parameter.name, parameterSchema);

                if(parameter.required) {
                    required.add(parameter.name);
                }
            }

            parameters.put("properties", properties);
            parameters.put("required", required);

            return parameters;
        }

        @Getter
        @AllArgsConstructor
        public enum ReasoningMode {
            functionCall("function_call"),
            prompt("prompt");

            private final String value;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ParameterConfig {
            private String name;
            private String type;
            private List<String> options;
            private String description;
            private boolean required;

            public void validate() {
                if(StringUtils.isEmpty(name)) {
                    throw new IllegalArgumentException("Parameter name is required");
                }
                if("__reason".equals(name) || "__is_success".equals(name)) {
                    throw new IllegalArgumentException("Invalid paramter name, __reason and __is_success are reserved");
                }
            }
        }
    }
}
