package com.ke.bella.workflow.node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.OpenAiUtils;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuestionClassifierNode extends BaseNode<QuestionClassifierNode.Data> {

    @SuppressWarnings("unchecked")
    public QuestionClassifierNode(Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
        meta.getData().put("source_handles_size", this.data.classes.size());
    }

    @Override
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {

        try {
            // 获取query
            Object query = context.getState().getVariableValue(data.getQueryVariableSelector());
            List<ChatMessage> chatMessages = getChatTemplate(context.getState().getVariablePool(), this.data, String.valueOf(query));

            if(data.getVision().enabledWithFiles()) {
                chatMessages = appendVisionMessages(chatMessages, data.getVision(), context.getState().getVariablePool());
            }

            // 构造参数请求LLM
            ChatCompletionResult compResult = invokeOpenAPILlm(data.getAuthorization(), chatMessages);
            String content = compResult.getChoices().isEmpty() ? "" : compResult.getChoices().get(0).getMessage().getContent();
            Data.ClassConfig resultClass = parseAndCheckJsonMarkdown(content, Data.ClassConfig.class);

            // 获取第一个节点为默认节点
            Data.ClassConfig category = data.getClasses().isEmpty() ? null : data.getClasses().get(0);

			// 获取分类结果
            if(resultClass.getId() != null && resultClass.getName() != null) {
                category = data.getClasses().stream().filter(c -> c.getId().equals(resultClass.getId())).findFirst().orElse(category);
            }

            // 适配协议
            Map<String, Object> variables = Maps.newHashMap();
            variables.put("query", query);

            Map<String, Object> outputData = Maps.newHashMap();
            outputData.put("class_name", category != null ? category.getName() : null);
            outputData.put("usage", compResult.getUsage());
            outputData.put("finish_reason", compResult.getChoices().isEmpty() ? null : compResult.getChoices().get(0).getFinishReason());

			Map<String, Object> processData = Maps.newHashMap();
            processData.put("model_mode", data.getModel().getMode());

            HashMap<String, Object> prompts = new HashMap<>();
            prompts.put("prompt_messages", chatMessages);
            processData.put("prompts", prompts);

            processData.put("usage", compResult.getUsage());

            return NodeRunResult.builder()
                    .status(NodeRunResult.Status.succeeded)
                    .inputs(variables)
                    .processData(processData)
                    .outputs(outputData)
                    .activatedSourceHandles(Lists.newArrayList(category.getId()))
                    .build();
        } catch (Exception e) {
            LOGGER.error("QuestionClassifierNode execute error ", e);
            return NodeRunResult.builder().status(NodeRunResult.Status.failed).error(e).build();
        }

    }

    private ChatCompletionResult invokeOpenAPILlm(Data.Authorization authorization, List<ChatMessage> chatMessages) {
        OpenAiService service = OpenAiUtils.defaultOpenAiService(authorization.getToken(), this.data.getTimeout().getRead(), TimeUnit.SECONDS);
        ChatCompletionRequest req = data.getModel().getTemplateCompletionParams();
        req.setMessages(chatMessages);
        req.setUser(String.valueOf(BellaContext.getOperator().getUserId()));
        return service.createChatCompletion(req);
    }

    private <T> T parseAndCheckJsonMarkdown(String content, Class<T> clazz) {
        try {
            return parseJsonMarkdown(content, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON from markdown: " + content);
        }
    }

    private <T> T parseJsonMarkdown(String content, Class<T> clazz) {
        // Remove the triple backticks if present
        String jsonStr = content.trim();
        int start = jsonStr.indexOf("```json");
        int length = "```json".length();
        int end = jsonStr.lastIndexOf("```");
        if(start != -1 && end != -1) {
            jsonStr = jsonStr.substring(start + length, end).trim();
            return JsonUtils.fromJson(jsonStr, clazz);
        } else if(start != -1 && end == -1 && jsonStr.endsWith("``")) {
            end = jsonStr.indexOf("``", start + length);
            jsonStr = jsonStr.substring(start + length, end).trim();
            return JsonUtils.fromJson(jsonStr, clazz);
        } else if(jsonStr.startsWith("{")) {
            return JsonUtils.fromJson(jsonStr, clazz);
        } else {
            throw new RuntimeException("Could not find JSON block in the output: " + content);
        }
    }

    private List<ChatMessage> getChatTemplate(Map variablesPool, Data nodeData, String query) {
        List<Data.ClassConfig> config = nodeData.getClasses();
        String categories = JsonUtils.toJson(config);
        ChatMessage systemMessage = new SystemMessage(QUESTION_CLASSIFIER_SYSTEM_PROMPT);
        ChatMessage userMessage = new UserMessage(QUESTION_CLASSIFIER_USER_PROMPT_1);
        ChatMessage assistantMessage = new AssistantMessage(QUESTION_CLASSIFIER_ASSISTANT_PROMPT_1);
        ChatMessage userMessage2 = new UserMessage(QUESTION_CLASSIFIER_USER_PROMPT_2);
        ChatMessage assistantMessage2 = new AssistantMessage(QUESTION_CLASSIFIER_ASSISTANT_PROMPT_2);
        ChatMessage userMessage3 = new UserMessage(
                String.format(QUESTION_CLASSIFIER_USER_PROMPT_3, query, categories, Variables.format(nodeData.getInstruction(), variablesPool)));
        LOGGER.trace("Question Classifier Chat Messages: {} {} {} {} {} {}", systemMessage, userMessage, assistantMessage, userMessage2,
                assistantMessage2, userMessage3);
        return Lists.newArrayList(systemMessage, userMessage, assistantMessage, userMessage2, assistantMessage2, userMessage3);
    }

    public static Map<String, Object> defaultConfig(Map<String, Object> filters) {
        return JsonUtils.fromJson("{\"type\":\"question-classifier\",\"config\":{\"instructions\":\"\"}}", new TypeReference<Map<String, Object>>() {
        });
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Data extends BaseNode.BaseNodeData {

        @JsonAlias("query_variable_selector")
        List<String> queryVariableSelector;
        String type;
        Model model;
        @Builder.Default
        Vision vision = new Vision();
        List<ClassConfig> classes;
        String instruction = "";
        @Builder.Default
        Timeout timeout = new Timeout();

        @Builder.Default
        Authorization authorization = new Authorization();

        @Override
        public List<String> getSourceHandles() {
            return classes.stream().map(ClassConfig::getId).collect(Collectors.toList());
        }

        @lombok.Getter
        @lombok.Setter
        public static class Timeout {
            int read = 600;
        }

        @lombok.Getter
        @lombok.Setter
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class ClassConfig {
            @JsonAlias("category_id")
            String id;
            @JsonAlias("category_name")
            String name;
        }
    }

    private static final String QUESTION_CLASSIFIER_SYSTEM_PROMPT = "\n"
            + "    ### Job Description',\n"
            + "    You are a text classification engine that analyzes text data and assigns categories based on user input or automatically determined categories.\n"
            + "    ### Task\n"
            + "    Your task is to assign one categories ONLY to the input text and only one category may be assigned returned in the output.Additionally, you need to extract the key words from the text that are related to the classification.\n"
            + "    ### Format\n"
            + "    The input text is in the variable text_field.Categories are specified as a category list with two filed category_id and category_name in the variable categories .Classification instructions may be included to improve the classification accuracy.\n"
            + "    ### Constraint\n" + "    DO NOT include anything other than the JSON array in your response.\n";

    private static final String QUESTION_CLASSIFIER_USER_PROMPT_1 = "\n"
            + "    { \"input_text\": [\"I recently had a great experience with your company. The service was prompt and the staff was very friendly.\"],\n"
            + "    \"categories\": [{\"category_id\":\"f5660049-284f-41a7-b301-fd24176a711c\",\"category_name\":\"Customer Service\"},{\"category_id\":\"8d007d06-f2c9-4be5-8ff6-cd4381c13c60\",\"category_name\":\"Satisfaction\"},{\"category_id\":\"5fbbbb18-9843-466d-9b8e-b9bfbb9482c8\",\"category_name\":\"Sales\"},{\"category_id\":\"23623c75-7184-4a2e-8226-466c2e4631e4\",\"category_name\":\"Product\"}],\n"
            + "    \"classification_instructions\": [\"classify the text based on the feedback provided by customer\"]}\n";

    private static final String QUESTION_CLASSIFIER_ASSISTANT_PROMPT_1 = "\n"
            + "```json\n"
            + "    {\"keywords\": [\"recently\", \"great experience\", \"company\", \"service\", \"prompt\", \"staff\", \"friendly\"],\n"
            + "    \"category_id\": \"f5660049-284f-41a7-b301-fd24176a711c\",\n"
            + "    \"category_name\": \"Customer Service\"}\n"
            + "```\n";

    private static final String QUESTION_CLASSIFIER_USER_PROMPT_2 = "\n"
            + "    {\"input_text\": [\"bad service, slow to bring the food\"],\n"
            + "    \"categories\": [{\"category_id\":\"80fb86a0-4454-4bf5-924c-f253fdd83c02\",\"category_name\":\"Food Quality\"},{\"category_id\":\"f6ff5bc3-aca0-4e4a-8627-e760d0aca78f\",\"category_name\":\"Experience\"},{\"category_id\":\"cc771f63-74e7-4c61-882e-3eda9d8ba5d7\",\"category_name\":\"Price\"}],\n"
            + "    \"classification_instructions\": []}\n";

    private static final String QUESTION_CLASSIFIER_ASSISTANT_PROMPT_2 = "\n" +
            "```json\n"
            + "    {\"keywords\": [\"bad service\", \"slow\", \"food\", \"tip\", \"terrible\", \"waitresses\"],\n"
            + "    \"category_id\": \"f6ff5bc3-aca0-4e4a-8627-e760d0aca78f\",\n"
            + "    \"category_name\": \"Experience\"}\n"
            + "```\n";

    private static final String QUESTION_CLASSIFIER_USER_PROMPT_3 = "\n"
            + "    '{{\"input_text\": [\"%s\"],',\n"
            + "    '\"categories\": %s, ',\n"
            + "    '\"classification_instructions\": [\"%s\"]}}'\n";

}
