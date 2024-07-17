package com.ke.bella.workflow.service.node;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ke.bella.workflow.service.Configs;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.IWorkflowCallback;
import com.ke.bella.workflow.service.JsonUtils;
import com.ke.bella.workflow.service.Variables;
import com.ke.bella.workflow.service.WorkflowContext;
import com.ke.bella.workflow.service.WorkflowRunState;
import com.ke.bella.workflow.service.WorkflowSchema;
import com.ke.bella.workflow.service.utils.HttpUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class KnowledgeRetrievalNode extends BaseNode {
    private Data data;

    private static final String FILES_RETRIEVE = "/files/retrieve";

    public KnowledgeRetrievalNode(WorkflowSchema.Node meta) {
        super(meta);
        this.data = JsonUtils.convertValue(meta.getData(), Data.class);
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
            List<KnowledgeRetrievalResult> retrieveResult = invokeFileRetrieve(query, data.getDatasetIds());

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

    private List<KnowledgeRetrievalResult> invokeFileRetrieve(String query, List<String> datasetIds) {
        Map<String, String> headers = Collections.singletonMap("Authorization", "Bearer " + BellaContext.getApiKey());
        String fileRetrieveUrl = Configs.API_BASE + FILES_RETRIEVE;

        Map<String, String> params = new HashMap<>();
        params.put("file_ids", datasetIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        params.put("query", query);

        BellaFileRetrieveResult bellaFileRetrieveResult = HttpUtils.postFrom(headers, fileRetrieveUrl, params,
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
            private Long id;
            @JsonAlias("file_id")
            private String fileId;
            @JsonAlias("file_name")
            private String fileName;
            @JsonAlias("chunkId")
            private String chunkId;
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
