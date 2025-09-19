package com.ke.bella.workflow.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
import java.time.LocalDateTime;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.service.IWorkflowRunLogService;
import com.ke.bella.workflow.service.IWorkflowRunLogService.WorkflowDailyStatistic;
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
    public Map<String, List<Map<String, Object>>> getDailyRunsStatistic(String workflowId, LocalDateTime startDate, LocalDateTime endDate) {

        // 获取过滤后的工作流运行记录
        List<WorkflowRunLog> logs = getFilteredWorkflowRunLogs(workflowId, startDate, endDate);

        // 按日期分组统计运行次数、终端用户数和平均交互次数
        Map<String, WorkflowDailyStatistic> dailyStats = calculateDailyStats(logs);

        // 生成完整的日期范围并补全缺失日期
        List<Map<String, Object>> data = generateCompleteDateRange(dailyStats, startDate, endDate);

        // 包装在data字段中
        Map<String, List<Map<String, Object>>> response = new HashMap<>();
        response.put("data", data);

        return response;
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
    public List<WorkflowRunLog> listNodeRunLogs(QueryOps ops) {
        return listWorkflowRuns(ops);
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

        // Add time range filtering support for ES
        if(ops.getStartTime() != null || ops.getEndTime() != null) {
            org.elasticsearch.index.query.RangeQueryBuilder timeRangeQuery = QueryBuilders.rangeQuery("ctime");

            if(ops.getStartTime() != null) {
                long startTimeMillis = ops.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                timeRangeQuery.gte(startTimeMillis);
            }

            if(ops.getEndTime() != null) {
                long endTimeMillis = ops.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                timeRangeQuery.lte(endTimeMillis);
            }

            boolQueryBuilder.must(timeRangeQuery);
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

    /**
     * 获取过滤后的工作流运行记录 (ES版本)
     * 优化版本：使用ES的时间过滤功能，避免内存过载
     */
    private List<WorkflowRunLog> getFilteredWorkflowRunLogs(String workflowId, LocalDateTime start, LocalDateTime end) {
        // 直接使用 QueryOps 的时间过滤功能，在ES层面过滤
        IWorkflowRunLogService.QueryOps queryOps = IWorkflowRunLogService.QueryOps.builder()
                .workflowId(workflowId)
                .triggerFroms(Lists.newArrayList(WorkflowOps.TriggerFrom.API.name(), WorkflowOps.TriggerFrom.CUSTOM_API.name(),
                        WorkflowOps.TriggerFrom.SCHEDULE.name(),
                        WorkflowOps.TriggerFrom.KAFKA.name()))
                .startTime(start)
                .endTime(end)
                .size(10000) // 设置合理的限制，避免无限制查询
                .orderBy("ctime")
                .order("asc") // 按时间升序，便于统计处理
                .build();

        // 执行单次查询，ES层面已经过滤了时间范围
        Page<WorkflowRunLog> page = this.pageWorkflowRunLogs(queryOps);
        return page.getData();
    }

    /**
     * 计算每日统计数据，包括运行次数和终端用户数
     */
    private Map<String, WorkflowDailyStatistic> calculateDailyStats(List<WorkflowRunLog> logs) {
        Map<String, WorkflowDailyStatistic> result = new HashMap<>();

        // 按日期分组
        Map<String, List<WorkflowRunLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(log -> formatDate(log.getCtime())));

        // 计算每个日期的统计数据
        for (Map.Entry<String, List<WorkflowRunLog>> entry : logsByDate.entrySet()) {
            String date = entry.getKey();
            List<WorkflowRunLog> dailyLogs = entry.getValue();

            // 计算运行次数
            long runCount = dailyLogs.size();

            // 计算不同用户数
            long terminalCount = dailyLogs.stream()
                    .map(WorkflowRunLog::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            // 计算平均交互次数
            BigDecimal interactions = calculateAverageInteractions(dailyLogs);
            // 使用WorkflowDailyStatistic存储统计结果
            WorkflowDailyStatistic statistic = WorkflowDailyStatistic.builder()
                    .date(date)
                    .runs(runCount)
                    .terminalCount(terminalCount)
                    .interactions(interactions)
                    .build();

            result.put(date, statistic);
        }

        return result;
    }

    /**
     * 计算平均交互次数
     * 先按用户分组计算每个用户的交互次数，然后计算平均值
     */
    private BigDecimal calculateAverageInteractions(List<WorkflowRunLog> logs) {
        if(logs.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 按用户ID分组，计算每个用户的交互次数
        Map<Long, Long> userInteractions = logs.stream()
                .filter(log -> log.getUserId() != null)
                .collect(Collectors.groupingBy(
                        WorkflowRunLog::getUserId,
                        Collectors.counting()));

        if(userInteractions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算所有用户交互次数的总和
        long totalInteractions = userInteractions.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        // 计算平均值并保留两位小数
        return BigDecimal.valueOf(totalInteractions)
                .divide(BigDecimal.valueOf(userInteractions.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 生成完整的日期范围并补全缺失日期
     */
    private List<Map<String, Object>> generateCompleteDateRange(
            Map<String, WorkflowDailyStatistic> dailyStats, LocalDateTime start, LocalDateTime end) {

        LocalDate startDate = start != null ? start.toLocalDate() : getEarliestDate(dailyStats.keySet());

        LocalDate endDate = end != null ? end.toLocalDate() : getLatestDate(dailyStats.keySet());

        // 生成所有日期并填充数据
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);

            // 获取当前日期的统计数据，如果不存在则创建默认值
            WorkflowDailyStatistic stats = dailyStats.getOrDefault(dateStr,
                    WorkflowDailyStatistic.builder()
                            .date(dateStr)
                            .runs(0L)
                            .terminalCount(0L)
                            .interactions(BigDecimal.ZERO)
                            .build());

            item.put("runs", stats.getRuns());
            item.put("terminal_count", stats.getTerminalCount());
            item.put("interactions", stats.getInteractions());

            result.add(item);

            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    /**
     * 获取数据中最早的日期
     */
    private LocalDate getEarliestDate(Set<String> dates) {
        if(dates.isEmpty()) {
            return LocalDate.now();
        }

        return dates.stream()
                .map(LocalDate::parse)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    /**
     * 获取数据中最晚的日期
     */
    private LocalDate getLatestDate(Set<String> dates) {
        if(dates.isEmpty()) {
            return LocalDate.now();
        }

        return dates.stream()
                .map(LocalDate::parse)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    /**
     * 将时间戳格式化为日期字符串 (YYYY-MM-DD)
     */
    private String formatDate(Long timestamp) {
        LocalDate date = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
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
        private String triggerId;
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
                    .triggerId(runLogEs.getTriggerId())
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
