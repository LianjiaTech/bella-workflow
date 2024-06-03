package com.ke.bella.workflow.node;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.JsonUtils;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class LlmNode extends BaseNode {

    private Data data;

    public LlmNode(WorkflowSchema.Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
    }

    @Override
    protected WorkflowRunState.NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map<String, Object> processData = new HashMap<>();
        try {
            validateData();
            Map<String, Object> nodeInputs = new HashMap<>();
            // only support chat model currently
            List<ChatMessage> chatMessages = fetchChatMessages(context);
            // fill process data
            processData = fillProcessData(chatMessages);
            // invoke llm
            Flowable<ChatCompletionChunk> llmResult = invokeLlm(chatMessages);
            String result = handleInvokeResult(data.getTimeout(), context, llmResult, callback);
            // fill outputs
            HashMap<String, Object> outputs = fillOutputs(result);
            return NodeRunResult.builder()
                    .status(NodeRunResult.Status.succeeded)
                    .inputs(nodeInputs)
                    .outputs(outputs)
                    .processData(processData).build();
        } catch (Exception e) {
            return NodeRunResult.builder()
                    .status(NodeRunResult.Status.failed)
                    .inputs(null)
                    .error(e)
                    .processData(processData).build();
        }
    }

    @NotNull
    private static HashMap<String, Object> fillOutputs(String result) {
        HashMap<String, Object> outputs = new HashMap<>();
        outputs.put("text", result);
        return outputs;
    }

    private static Map<String, Object> fillProcessData(List<ChatMessage> chatMessages) {
        HashMap<String, Object> prompts = new HashMap<>();
        prompts.put("model_mode", "chat");
        prompts.put("prompt_messages", chatMessages);

        Map<String, Object> processData = new HashMap<>();
        processData.put("model_mode", "chat");
        processData.put("prompts", prompts);
        return processData;
    }

    private void validateData() {
        Assert.notNull(data.getAuthorization(), "authorization不能为空");
        data.getAuthorization().valid();
    }

    private String handleInvokeResult(Data.Timeout timeout, WorkflowContext context, Flowable<ChatCompletionChunk> llmResult,
            IWorkflowCallback callback) {
        StringBuilder fullText = new StringBuilder();
        CompletableFuture<String> completionFuture = new CompletableFuture<>();
        // todo usage
        Disposable subscribe = llmResult.subscribe(chunk -> {
            String content = chunk.getChoices().get(0).getMessage().getContent();
            fullText.append(content);
            callback.onWorkflowNodeRunProgress(context, meta.getId(),
                    IWorkflowCallback.ProgressData.builder().data(chunk).build());
        }, completionFuture::completeExceptionally, () -> completionFuture.complete(fullText.toString()));
        try {
            return completionFuture.get(timeout.getReadSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            if(!subscribe.isDisposed()) {
                subscribe.dispose();
            }
            throw new RuntimeException(e);
        }
    }

    private Flowable<ChatCompletionChunk> invokeLlm(List<ChatMessage> chatMessages) {
        OpenAiService service = new OpenAiService(data.getAuthorization().authorization(), Duration.ofSeconds(data.getTimeout().getReadSeconds()),
                data.getAuthorization().getApiBaseUrl());
        ChatCompletionRequest chatCompletionRequest = JsonUtils.fromJson(JsonUtils.toJson(data.getModel().getCompletionParams()),
                ChatCompletionRequest.class);
        chatCompletionRequest.setMessages(chatMessages);
        chatCompletionRequest.setModel(data.getModel().getName());
        return service.streamChatCompletion(chatCompletionRequest);
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Object> fetchJinjaInputs(Data data, Map variablePool) {
        HashMap<String, Object> result = new HashMap<>();
        for (WorkflowSchema.Variable jinja2Variable : data.getPromptConfig().getJinja2Variables()) {
            Object value = Variables.getValue(variablePool, jinja2Variable.getValueSelector());
            result.put(jinja2Variable.getVariable(), value);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private List<ChatMessage> fetchChatMessages(WorkflowContext context) {
        List<ChatMessage> result = new ArrayList<>();
        Map variablePool = context.getState().getVariablePool();
        for (Data.PromptTemplate promptTemplate : data.promptTemplate) {
            if("jinja2".equals(promptTemplate.getEditionType())) {
                Map<String, Object> jinjaInputs = fetchJinjaInputs(data, variablePool);
                String textFormatted = Variables.renderJinjia(promptTemplate.getJinja2Text(), jinjaInputs);
                promptTemplate.setText(textFormatted);
            } else if("basic".equals(promptTemplate.getEditionType()) || StringUtils.isEmpty(promptTemplate.getEditionType())) {
                String text = promptTemplate.getText();
                promptTemplate.setText(Variables.format(text, variablePool));
            } else {
                throw new IllegalArgumentException("Invalided edition type: " + promptTemplate.getEditionType());
            }
            if(promptTemplate.getRole().equals("user")) {
                result.add(new UserMessage(promptTemplate.getText()));
            } else if(promptTemplate.getRole().equals("system")) {
                result.add(new SystemMessage(promptTemplate.getText()));
            } else if(promptTemplate.getRole().equals("assistant")) {
                result.add(new AssistantMessage(promptTemplate.getText()));
            }
        }
        return result;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Data extends BaseNodeData {
        private String title;
        private String desc;
        private Model model;
        @JsonAlias("prompt_template")
        private List<PromptTemplate> promptTemplate;
        @JsonAlias("prompt_config")
        private PromptConfig promptConfig;
        Context context;
        private Vision vision;
        // todo impl memory
        private Object memory;
        @Builder.Default
        private Timeout timeout = new Timeout();
        private Authorization authorization;

        @lombok.Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Authorization {
            String apiKey;
            String apiBaseUrl = "https://example.com/v1/";

            public String authorization() {
                return String.format("Bearer %s", apiKey);
            }

            public void valid() {
                Assert.hasText(apiKey, "apiKey不能为空");
            }
        }

        @lombok.Getter
        @lombok.Setter
        public static class Timeout {
            int readSeconds = 600;
        }

        @lombok.Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Model {
            private String provider;
            private String name;
            private String mode;
            @JsonAlias("completion_params")
            private Map<String, Object> completionParams;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class PromptTemplate {
            private String text;
            private String role;
            @JsonAlias("edition_type")
            private String editionType;
            @JsonAlias("jinja2_text")
            private String jinja2Text;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class PromptConfig {
            @JsonAlias("jinja2_variables")
            private List<WorkflowSchema.Variable> jinja2Variables;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Context {
            boolean enabled;
            @JsonAlias("variable_selector")
            private List<String> variableSelector;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Vision {
            boolean enabled;
            private List<Object> configs;
        }
    }

}
