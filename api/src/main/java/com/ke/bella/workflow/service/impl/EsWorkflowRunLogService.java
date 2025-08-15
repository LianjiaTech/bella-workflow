package com.ke.bella.workflow.service.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Throwables;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.service.IWorkflowRunLogService;
import com.ke.bella.workflow.service.WorkflowRunCallback;
import com.ke.bella.workflow.service.WorkflowRunCallback.WorkflowRunLog;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EsWorkflowRunLogService implements IWorkflowRunLogService {

    private final RestHighLevelClient esClient;
    private final String logIndex;

    public EsWorkflowRunLogService(RestHighLevelClient esClient, String logIndex) {
        this.esClient = esClient;
        this.logIndex = logIndex;
    }

    @Override
    public WorkflowRunLog getWorkflowRunLog(String workflowRunId) {
        QueryOps build = QueryOps.builder()
                .workflowRunId(workflowRunId)
                .build();
        List<WorkflowRunLog> workflowRunLogs = listWorkflowRuns(build);

        if(CollectionUtils.isEmpty(workflowRunLogs)) {
            return null;
        }

        WorkflowRunLog logAggregation = new WorkflowRunLog();
        for (WorkflowRunLog workflowRunLog : workflowRunLogs) {
            if(WorkflowRunCallback.WorkflowRunEvent.onWorkflowRunSucceeded.name().equals(workflowRunLog.getEvent())) {
                logAggregation.setOutputs(workflowRunLog.getOutputs());
                logAggregation.setStatus(WorkflowRunState.WorkflowRunStatus.succeeded.name());
                logAggregation.setElapsedTime(workflowRunLog.getElapsedTime());

            } else if(WorkflowRunCallback.WorkflowRunEvent.onWorkflowRunFailed.name().equals(workflowRunLog.getEvent())) {
                logAggregation.setStatus(WorkflowRunState.WorkflowRunStatus.failed.name());
                logAggregation.setError(workflowRunLog.getError());
                logAggregation.setElapsedTime(workflowRunLog.getElapsedTime());

            } else if(WorkflowRunCallback.WorkflowRunEvent.onWorkflowRunStarted.name().equals(workflowRunLog.getEvent())) {
                logAggregation.setBellaTraceId(workflowRunLog.getBellaTraceId());
                logAggregation.setAkCode(workflowRunLog.getAkCode());
                logAggregation.setTenantId(workflowRunLog.getTenantId());
                logAggregation.setUserId(workflowRunLog.getUserId());
                logAggregation.setUserName(workflowRunLog.getUserName());
                logAggregation.setWorkflowId(workflowRunLog.getWorkflowId());
                logAggregation.setWorkflowRunId(workflowRunLog.getWorkflowRunId());
                logAggregation.setFlashMode(workflowRunLog.getFlashMode());
                logAggregation.setTriggerFrom(workflowRunLog.getTriggerFrom());
                logAggregation.setThreadId(workflowRunLog.getThreadId());
                logAggregation.setStateful(workflowRunLog.isStateful());
                logAggregation.setSys(workflowRunLog.getSys());
                logAggregation.setInputs(workflowRunLog.getInputs());
                logAggregation.setCtime(workflowRunLog.getCtime());
            }
        }

        return logAggregation;
    }

    @Override
    public Page<WorkflowRunLog> pageWorkflowRunLogs(QueryOps ops) {
        try {
            SearchRequest request = getSearchRequest(ops);
            SearchResponse response = esClient.search(request, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] hits1 = hits.getHits();
            List<WorkflowRunLog> workflowRunLogs = Arrays.stream(hits1)
                    .map(s -> {
                        try {
                            StreamInput streamInput = s.getSourceRef().streamInput();
                            return JsonUtils.fromJson(streamInput, WorkflowRunLogEs.class);
                        } catch (IOException e) {
                            LOGGER.warn("failed to parse workflow run log, log: {}, e: {}", s, Throwables.getStackTraceAsString(e));
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(WorkflowRunLogEs::transfer)
                    .collect(Collectors.toList());

            return Page.<WorkflowRunLog>from((ops.getFromIndex() + 1) % ops.getSize(), ops.getSize()).list(workflowRunLogs)
                    .total(Math.toIntExact(hits.getTotalHits().value));
        } catch (Exception e) {
            LOGGER.error("failed to search workflow run logs", e);
            throw new IllegalStateException("failed to search workflow run logs", e);
        }
    }

    @Override
    public void saveWorkflowRunLog(WorkflowRunLog runLog) {
        // ignore for elasticsearch - logs are saved via logback appender
    }

    public List<WorkflowRunLog> listWorkflowRuns(QueryOps ops) {
        return pageWorkflowRunLogs(ops).getData();
    }

    private SearchRequest getSearchRequest(QueryOps ops) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if(!CollectionUtils.isEmpty(ops.getStatus())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("status", ops.getStatus()));
        }
        if(!StringUtils.isEmpty(ops.getWorkflowId())) {
            boolQueryBuilder.must(QueryBuilders.termQuery("workflowId", ops.getWorkflowId()));
        }
        if(!StringUtils.isEmpty(ops.getWorkflowRunId())) {
            boolQueryBuilder.must(QueryBuilders.termQuery("workflowRunId", ops.getWorkflowRunId()));
        }
        if(ops.getUserId() != null) {
            boolQueryBuilder.must(QueryBuilders.termQuery("userId", ops.getUserId()));
        }
        if(!CollectionUtils.isEmpty(ops.getTriggerFroms())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("triggerFrom", ops.getTriggerFroms()));
        }
        if(!CollectionUtils.isEmpty(ops.getEvents())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("event", ops.getEvents()));
        }

        SearchRequest request = new SearchRequest();
        request.searchType(SearchType.DEFAULT);
        request.indices(logIndex);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(boolQueryBuilder);
        sourceBuilder.sort(ops.getOrderBy(), SortOrder.fromString(ops.getOrder()));
        sourceBuilder.fetchSource(true);

        if(ops.getFromIndex() != null) {
            sourceBuilder.from(ops.getFromIndex());
        }
        if(ops.getSize() != null) {
            sourceBuilder.size(ops.getSize());
        }

        request.source(sourceBuilder);

        return request;
    }

    @Data
    @NoArgsConstructor
    public static class WorkflowRunLogEs {
        private String bellaTraceId;
        private String akCode;
        private String event;
        private String tenantId;
        private Long userId;
        private String userName;
        private String workflowId;
        private String workflowRunId;
        private int flashMode;
        private String triggerFrom;
        private String threadId;
        private boolean stateful;
        private String sys;
        private String inputs;
        private String outputs;
        private String status;
        private Long ctime;
        private Long elapsedTime;
        private String nodeId;
        private String nodeType;
        private String nodeTitle;
        private String nodeRunId;
        private String nodeInputs;
        private String nodeProcessData;
        private String nodeOutputs;
        private String error;
        private boolean iteration;
        private Integer iterationIndex;

        public static WorkflowRunLog transfer(WorkflowRunLogEs runLogEs) {
            return WorkflowRunLog.builder()
                    .bellaTraceId(runLogEs.getBellaTraceId())
                    .akCode(runLogEs.getAkCode())
                    .event(runLogEs.getEvent())
                    .tenantId(runLogEs.getTenantId())
                    .userId(runLogEs.getUserId())
                    .userName(runLogEs.getUserName())
                    .workflowId(runLogEs.getWorkflowId())
                    .workflowRunId(runLogEs.getWorkflowRunId())
                    .flashMode(runLogEs.getFlashMode())
                    .triggerFrom(runLogEs.getTriggerFrom())
                    .threadId(runLogEs.getThreadId())
                    .stateful(runLogEs.isStateful())
                    .sys(JsonUtils.fromJson(runLogEs.getSys(), Object.class))
                    .inputs(JsonUtils.fromJson(runLogEs.getInputs(), Object.class))
                    .outputs(JsonUtils.fromJson(runLogEs.getOutputs(), Object.class))
                    .status(runLogEs.getStatus())
                    .ctime(runLogEs.getCtime())
                    .elapsedTime(runLogEs.getElapsedTime())
                    .nodeId(runLogEs.getNodeId())
                    .nodeType(runLogEs.getNodeType())
                    .nodeTitle(runLogEs.getNodeTitle())
                    .nodeRunId(runLogEs.getNodeRunId())
                    .nodeInputs(JsonUtils.fromJson(runLogEs.getNodeInputs(), Object.class))
                    .nodeProcessData(JsonUtils.fromJson(runLogEs.getNodeProcessData(), Object.class))
                    .nodeOutputs(JsonUtils.fromJson(runLogEs.getNodeOutputs(), Object.class))
                    .error(runLogEs.getError())
                    .iteration(runLogEs.isIteration())
                    .iterationIndex(runLogEs.getIterationIndex())
                    .build();
        }
    }
}
