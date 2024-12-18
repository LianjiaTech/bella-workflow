package com.ke.bella.workflow.node;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.Variables;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.node.BaseNode.BaseNodeData;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class KnowledgeRetrievalNode extends BaseNode<KnowledgeRetrievalNode.Data> {
    private static final String FILES_RETRIEVE = "api/rag/retrieval";

    public KnowledgeRetrievalNode(WorkflowSchema.Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
    }

    @Override
    protected WorkflowRunState.NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        String query = Variables.getValueAsString(context.getState().getVariablePool(), data.getQueryVariableSelector());
        Map<String, Object> inputs = Collections.singletonMap("query", query);

        if(StringUtils.isEmpty(query)) {
            return WorkflowRunState.NodeRunResult.builder()
                    .inputs(inputs)
                    .status(WorkflowRunState.NodeRunResult.Status.failed)
                    .error(new IllegalArgumentException("query is required")).build();
        }
        try {
            List<KnowledgeRetrievalResult> retrieveResult = invokeFileRetrieve(query, data.getDatasetIds(), data.multipleRetrievalConfig.getTopK(),
                    data.multipleRetrievalConfig.getScoreThreshold(), data.multipleRetrievalConfig.getRetrievalMode(),
                    data.multipleRetrievalConfig.isBackground());

            Map<String, Object> outputs = Collections.singletonMap("result", retrieveResult);
            return WorkflowRunState.NodeRunResult.builder()
                    .status(WorkflowRunState.NodeRunResult.Status.succeeded)
                    .inputs(inputs)
                    .outputs(outputs).build();
        } catch (Exception e) {
            return WorkflowRunState.NodeRunResult.builder()
                    .status(WorkflowRunState.NodeRunResult.Status.failed)
                    .error(e)
                    .inputs(inputs).build();
        }
    }

    private List<KnowledgeRetrievalResult> invokeFileRetrieve(String query, List<String> datasetIds, Integer topK, Float scoreThreshold,
            String retrievalMode,
            boolean background) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + BellaContext.getApiKey());
        Optional.ofNullable(BellaContext.getTransHeaders()).ifPresent(map -> map.forEach(headers::putIfAbsent));
        String fileRetrieveUrl = Configs.OPEN_API_BASE + FILES_RETRIEVE;

        KnowledgeRetrievalRequest request = new KnowledgeRetrievalRequest();
        request.setFileIds(datasetIds);
        request.setQuery(query);
        request.setTopK(topK);
        request.setScore(scoreThreshold);
        request.setRetrieveMode(retrievalMode);
        request.setUser(String.valueOf(BellaContext.getOperator().getUserId()));

        if(background) {
            List<KnowledgeRetrievalRequest.Plugin> plugins = Collections.singletonList(contextCompletePlugin());
            request.setPlugins(plugins);
        }

        BellaFileRetrieveResult bellaFileRetrieveResult = HttpUtils.postJson(headers, fileRetrieveUrl, JsonUtils.toJson(request),
                new TypeReference<BellaFileRetrieveResult>() {
                });
        if(Objects.isNull(bellaFileRetrieveResult) || StringUtils.hasText(bellaFileRetrieveResult.getErrno())) {
            throw new IllegalStateException(
                    String.format("invoke bella file retrieve error, response body: %s", JsonUtils.toJson(bellaFileRetrieveResult)));
        }
        List<BellaFileRetrieveResult.Chunk> retrieveResult = bellaFileRetrieveResult.getList();
        if(CollectionUtils.isEmpty(retrieveResult)) {
            return Collections.emptyList();
        }
        return retrieveResult.stream().map(KnowledgeRetrievalResult::transfer).collect(Collectors.toList());
    }

    private static KnowledgeRetrievalRequest.Plugin contextCompletePlugin() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("complete_mode", "context_complete");
        return KnowledgeRetrievalRequest.Plugin.builder()
                .name("completer")
                .parameters(params)
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Data extends BaseNodeData {
        @JsonAlias("query_variable_selector")
        private List<String> queryVariableSelector;

        @JsonAlias("dataset_ids")
        private List<String> datasetIds;

        @Builder.Default
        @JsonAlias("multiple_retrieval_config")
        private MultipleRetrievalConfig multipleRetrievalConfig = new MultipleRetrievalConfig();
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
        private float scoreThreshold = 0.8f;

        @JsonAlias("retrieval_mode")
        private String retrievalMode = "semantic";

        private boolean background = true;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BellaFileRetrieveResult {
        private String errno;
        private String errmsg;
        List<Chunk> list;
        private String id;
        private String object;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Chunk {
            private String id;
            @JsonAlias("file_id")
            private String fileId;
            @JsonAlias("file_name")
            private String fileName;
            @JsonAlias("chunkId")
            private String chunkId;
            @JsonAlias("score")
            private Float score;
            private String content;
            @JsonAlias("file_tag")
            private String fileTag;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeRetrievalRequest {
        @JsonProperty("file_ids")
        private List<String> fileIds;
        private String query;
        @JsonProperty("top_k")
        private Integer topK;
        private Float score;
        private String user;
        @JsonProperty("retrieve_mode")
        private String retrieveMode;
        private List<Plugin> plugins;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Plugin {
            private String name;
            private Map parameters;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeRetrievalResult {
        private String content;
        private String title;
        private String url;
        private String icon;
        private Map<String, Object> metadata;

        @SuppressWarnings("unchecked")
        public static KnowledgeRetrievalResult transfer(BellaFileRetrieveResult.Chunk bellaResult) {
            return KnowledgeRetrievalResult.builder()
                    .content(bellaResult.getContent())
                    .title(bellaResult.getFileName())
                    .metadata(JsonUtils.fromJson(JsonUtils.toJson(bellaResult), Map.class))
                    .build();
        }
    }
}
