package com.ke.bella.workflow.node;

import static com.ke.bella.workflow.IWorkflowCallback.ProgressData.EventType.MESSAGE_COMPLETED;
import static com.ke.bella.workflow.IWorkflowCallback.ProgressData.EventType.MESSAGE_DELTA;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.JsonUtils;
import com.theokanning.openai.assistants.message.MessageContent;
import com.theokanning.openai.assistants.message.content.DeltaContent;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.sse.RealEventSource;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

@Slf4j
public class RagNode extends BaseNode<RagNode.Data> {
    static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    static OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectionPool(new ConnectionPool(1500, 60, TimeUnit.SECONDS))
            .build();

    private static final String RAG_PATH = "/api/rag/query";
    private static final Long DEFAULT_READ_TIMEOUT_SECONDS = 60 * 5L;

    private long ttftStart;
    private long ttftConnect;
    private long ttftEnd;
    private static final String X_BELLA_REQUEST_ID = "X-BELLA-REQUEST-ID";

    public RagNode(WorkflowSchema.Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected WorkflowRunState.NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {

        String query = Variables.getValueAsString(context.getState().getVariablePool(), data.getQueryVariableSelector());
        Map<String, Object> inputs = Collections.singletonMap("query", query);

        if(StringUtils.isEmpty(query)) {
            return WorkflowRunState.NodeRunResult.builder()
                    .inputs(inputs)
                    .status(WorkflowRunState.NodeRunResult.Status.failed)
                    .error(new IllegalArgumentException("query is required")).build();
        }

        Map processedData = new LinkedHashMap<>();

        try {
            String instruction = null;
            if(!StringUtils.isEmpty(data.getGenerationConfig().getInstruction())) {
                instruction = Variables.format(data.getGenerationConfig().getInstruction(), context.getState().getVariablePool());
            }

            BellaRagParams params = getBellaRagParams(query, data.getDatasetIds(), data.multipleRetrievalConfig.getTopK(),
                    data.multipleRetrievalConfig.getScoreThreshold(), data.getGenerationConfig().getModel(), instruction);
            processedData.put("params", params);

            List<MessageContent> contents = invokeBellaRag(params, context, callback);

            LlmNode.ProcessMetaData metadata = LlmNode.ProcessMetaData.builder()
                    .ttlt((System.nanoTime() - ttftStart) / 1000000L)
                    .ttft((this.ttftEnd - ttftStart) / 1000000L)
                    .build();
            processedData.put("meta_data", metadata);

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("contents", contents);

            return WorkflowRunState.NodeRunResult.builder()
                    .status(WorkflowRunState.NodeRunResult.Status.succeeded)
                    .inputs(inputs)
                    .processData(processedData)
                    .outputs(outputs).build();
        } catch (Exception e) {
            return WorkflowRunState.NodeRunResult.builder()
                    .status(WorkflowRunState.NodeRunResult.Status.failed)
                    .error(e)
                    .inputs(inputs)
                    .processData(processedData).build();
        }
    }

    private List<MessageContent> invokeBellaRag(BellaRagParams params, WorkflowContext context, IWorkflowCallback callback) {
        try {

            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + BellaContext.getApiKey());
            headers.put(X_BELLA_REQUEST_ID, nodeRunId);
            Optional.ofNullable(BellaContext.getTransHeaders()).ifPresent(map -> map.forEach(headers::putIfAbsent));

            Request request = new Request.Builder()
                    .url(Configs.OPEN_API_BASE + RAG_PATH)
                    .headers(Headers.of(headers))
                    .method("POST", RequestBody.create(JsonUtils.toJson(params), MediaType.parse("application/json")))
                    .build();

            CompletableFuture<List<MessageContent>> ragFuture = new CompletableFuture<>();

            final String messageId = data.isGenerateNewMessage() ? context.newMessageId()
                    : (String) context.getState().getVariable("sys", "message_id");

            RealEventSource eventSource = new RealEventSource(request, new EventSourceListener() {

                private List<MessageContent> result;

                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                    ttftConnect = System.nanoTime();
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String rawdata) {
                    try {
                        RagStreamingResponse ragStreamingResponse = JsonUtils.fromJson(rawdata, new TypeReference<RagStreamingResponse>() {
                        });

                        if(ragStreamingResponse == null) {
                            LOGGER.info("[RagNode] onEvent type = {}, response= {}, no-op", type, rawdata);
                            return;
                        }

                        if(MESSAGE_DELTA.equals(type)) {
                            callback.onWorkflowNodeRunProgress(context, getNodeId(), nodeRunId,
                                    IWorkflowCallback.ProgressData.builder().object(ragStreamingResponse.getObject())
                                            .data(IWorkflowCallback.Delta.builder().messageId(messageId)
                                                    .content(ragStreamingResponse.getDelta()).build())
                                            .build());
                        } else if(MESSAGE_COMPLETED.equals(type)) {
                            result = ragStreamingResponse.getContent();
                        } else {
                            callback.onWorkflowNodeRunProgress(context, getNodeId(), nodeRunId,
                                    IWorkflowCallback.ProgressData.builder().object(ragStreamingResponse.getObject()).data(ragStreamingResponse)
                                            .build());
                        }

                    } catch (Exception e) {
                        ragFuture.completeExceptionally(e);
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    ttftEnd = System.nanoTime();
                    if(!ragFuture.isDone()) {
                        ragFuture.complete(result);
                    }
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    ttftEnd = System.nanoTime();
                    String errMsg = null;
                    try {
                        if(response.body() != null) {
                            errMsg = response.body().string();
                        } else if(t != null) {
                            errMsg = t.getMessage();
                        } else {
                            errMsg = "未知异常";
                        }

                        if(!ragFuture.isDone()) {
                            ragFuture.completeExceptionally(new IllegalStateException(errMsg));
                        } else {
                            LOGGER.info("[RagNode] future is done but still onFailure, errMsg = {}", errMsg);
                        }
                    } catch (Exception e) {
                        ragFuture.completeExceptionally(new IllegalStateException(errMsg));
                    }
                }
            });

            this.ttftStart = System.nanoTime();
            eventSource.connect(client);
            return ragFuture.get(DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("invoke bella rag error, error: %s", Throwables.getStackTraceAsString(e)));
        }
    }

    private static BellaRagParams getBellaRagParams(String query, List<String> datasetIds, Integer topK, Float scoreThreshold, Model model,
            String instruction) {
        BellaRagParams.RetrievalParam retrievalParam = BellaRagParams.RetrievalParam.builder()
                .fileIds(datasetIds.stream().map(String::valueOf).collect(Collectors.toList()))
                .score(scoreThreshold)
                .topK(topK).build();

        BellaRagParams.GenerateParam generateParam = BellaRagParams.GenerateParam.generateParam(model.getCompletionParams(), model, instruction);

        BellaRagParams params = BellaRagParams.builder()
                .query(query)
                .user(String.valueOf(BellaContext.getOperator().getUserId()))
                .retrieval_param(retrievalParam)
                .generate_param(generateParam).build();
        return params;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MultipleRetrievalConfig {
        @Builder.Default
        @JsonAlias("top_k")
        private int topK = 5;

        @Builder.Default
        @JsonAlias("score_threshold")
        private float scoreThreshold = 0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Data extends BaseNode.BaseNodeData {
        @JsonAlias("query_variable_selector")
        private List<String> queryVariableSelector;
        @JsonAlias("dataset_ids")
        private List<String> datasetIds;
        @JsonAlias("multiple_retrieval_config")
        private MultipleRetrievalConfig multipleRetrievalConfig = new MultipleRetrievalConfig();
        @JsonAlias("generation_config")
        private GenerationConfig generationConfig = new GenerationConfig();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerationConfig {
        @JsonAlias("model")
        private Model model;
        @JsonAlias("instruction")
        private String instruction;
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
    @AllArgsConstructor
    @Builder
    public static class BellaRagParams {
        @Nullable
        private RetrievalParam retrieval_param;
        @Nullable
        private GenerateParam generate_param;
        private String query;
        private String user;
        @Builder.Default
        private boolean stream = true;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class RetrievalParam {
            @JsonProperty("file_ids")
            private List<String> fileIds;
            private Float score;
            @JsonProperty("top_k")
            private Integer topK;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class GenerateParam extends ChatCompletionRequest {
            private String instructions;

            public static GenerateParam generateParam(Map<String, Object> completionParams, Model model, String instructions) {
                BellaRagParams.GenerateParam generateParam = Optional.ofNullable(JsonUtils.fromJson(
                        JsonUtils.toJson(completionParams),
                        new TypeReference<BellaRagParams.GenerateParam>() {
                        })).orElse(new GenerateParam());

                generateParam.setModel(model.getName());
                generateParam.setInstructions(instructions);

                return generateParam;
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RagStreamingResponse {
        private String id;
        private String object;
        private List<DeltaContent> delta;
        private List<MessageContent> content;
        private List<RagRetrievalDoc> doc;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RagRetrievalDoc {
        private String id;
        private Map metadata;
        private Double score;
        private String content;
        private String type;
    }
}
