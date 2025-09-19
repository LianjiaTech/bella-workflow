package com.ke.bella.workflow.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.google.common.collect.Lists;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunPage;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.service.IWorkflowRunLogService;
import com.ke.bella.workflow.service.WorkflowRunCallback.WorkflowRunLog;
import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DbWorkflowRunLogService implements IWorkflowRunLogService {

    private final WorkflowService ws;

    public DbWorkflowRunLogService(WorkflowService ws) {
        this.ws = ws;
    }

    @Override
    public Map<String, List<Map<String, Object>>> getDailyRunsStatistic(String workflowId, LocalDateTime start, LocalDateTime end) {

        // 获取过滤后的工作流运行记录
        List<WorkflowRunLog> logs = getFilteredWorkflowRunLogs(workflowId, start, end);

        // 按日期分组统计运行次数、终端用户数和平均交互次数
        Map<String, WorkflowDailyStatistic> dailyStats = calculateDailyStats(logs);

        // 生成完整的日期范围并补全缺失日期
        List<Map<String, Object>> data = generateCompleteDateRange(dailyStats, start, end);

        // 包装在data字段中
        Map<String, List<Map<String, Object>>> response = new HashMap<>();
        response.put("data", data);

        return response;
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
            // 计算平均交互次数 (类似Python代码中的逻辑)
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

    /**
     * 获取过滤后的工作流运行记录
     * 优化版本：使用数据库层面的时间过滤，避免内存过载
     */
    private List<WorkflowRunLog> getFilteredWorkflowRunLogs(String workflowId, LocalDateTime start, LocalDateTime end) {
        // 直接使用 QueryOps 的时间过滤功能，在数据库层面过滤
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

        // 执行单次查询，数据库层面已经过滤了时间范围
        Page<WorkflowRunLog> page = this.pageWorkflowRunLogs(queryOps);
        return page.getData();
    }

    @Override
    public WorkflowRunLog getWorkflowRunLog(String workflowRunId) {
        WorkflowRunDB workflowRun = ws.getWorkflowRun(workflowRunId);
        if(workflowRun == null) {
            return null;
        }
        return transferToWorkflowRunLog(workflowRun);
    }

    @Override
    public Page<WorkflowRunLog> pageWorkflowRunLogs(QueryOps ops) {
        // Check if querying node execution logs
        if(ops.getEvents() != null && ops.getEvents().stream().anyMatch(event -> event.contains("NodeRun"))
                && ops.getWorkflowRunId() != null) {
            return pageNodeRunLogs(ops);
        }

        // Query workflow run logs with time filtering support
        WorkflowRunPage.WorkflowRunPageBuilder pageOpsBuilder = WorkflowRunPage.builder()
                .workflowId(ops.getWorkflowId())
                .page(ops.getFromIndex() != null ? (ops.getFromIndex() / ops.getSize()) + 1 : 1)
                .pageSize(ops.getSize() != null ? ops.getSize() : 30)
                .lastId(ops.getLastWorkflowRunId());

        // Add time filtering if provided
        if(ops.getStartTime() != null) {
            pageOpsBuilder.startTime(ops.getStartTime());
        }

        // Note: WorkflowRunPage currently only supports startTime, not endTime
        // For complete time range filtering, we may need to enhance
        // WorkflowRunPage
        // or implement additional filtering at service layer

        WorkflowRunPage pageOps = pageOpsBuilder.build();
        Page<WorkflowRunDB> dbPage = ws.listWorkflowRun(pageOps);

        List<WorkflowRunLog> logList = dbPage.getData().stream()
                .map(this::transferToWorkflowRunLog)
                // Apply endTime filtering at service layer if needed
                .filter(log -> ops.getEndTime() == null ||
                        log.getCtime() <= ops.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .collect(Collectors.toList());

        return Page.<WorkflowRunLog>from(dbPage.getPage(), dbPage.getPageSize())
                .total(dbPage.getTotal())
                .list(logList);
    }

    private Page<WorkflowRunLog> pageNodeRunLogs(QueryOps ops) {
        List<WorkflowNodeRunDB> nodeRuns = ws.getNodeRuns(ops.getWorkflowRunId());
        List<WorkflowRunLog> logList = nodeRuns.stream()
                .map(this::transferNodeRunToWorkflowRunLog)
                .collect(Collectors.toList());

        return Page.<WorkflowRunLog>from(1, logList.size())
                .total(logList.size())
                .list(logList);
    }

    @Override
    public void saveWorkflowRunLog(WorkflowRunLog runLog) {
        // For DB mode, workflow run logs are stored in the database
        // automatically
        // through the workflow execution process, so no additional saving is
        // needed
        LOGGER.debug("saveWorkflowRunLog called for workflowRunId: {}, no action needed for DB mode",
                runLog.getWorkflowRunId());
    }

    private WorkflowRunLog transferToWorkflowRunLog(WorkflowRunDB wr) {
        return WorkflowRunLog.builder()
                .bellaTraceId(wr.getTraceId())
                .tenantId(wr.getTenantId())
                .userId(wr.getCuid())
                .userName(wr.getCuName())
                .workflowId(wr.getWorkflowId())
                .workflowRunId(wr.getWorkflowRunId())
                .flashMode(wr.getFlashMode() != null ? wr.getFlashMode() : 0)
                .triggerFrom(wr.getTriggerFrom())
                .threadId(wr.getThreadId())
                .stateful(wr.getStateful() != null && wr.getStateful() == 1)
                .status(wr.getStatus())
                .ctime(wr.getCtime() != null ? wr.getCtime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .elapsedTime(wr.getElapsedTime())
                .error(wr.getError())
                .inputs(parseJson(wr.getInputs()))
                .outputs(parseJson(wr.getOutputs()))
                .sys(parseJson(wr.getMetadata()))
                .event(getEventFromStatus(wr.getStatus()))
                .build();
    }

    private WorkflowRunLog transferNodeRunToWorkflowRunLog(WorkflowNodeRunDB nr) {
        return WorkflowRunLog.builder()
                .tenantId(nr.getTenantId())
                .workflowId(nr.getWorkflowId())
                .workflowRunId(nr.getWorkflowRunId())
                .nodeId(nr.getNodeId())
                .nodeRunId(nr.getNodeRunId())
                .nodeType(nr.getNodeType())
                .nodeTitle(nr.getTitle())
                .status(nr.getStatus())
                .elapsedTime(nr.getElapsedTime())
                .error(nr.getError())
                .ctime(nr.getCtime() != null ? nr.getCtime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .nodeInputs(parseJson(nr.getInputs()))
                .nodeOutputs(parseJson(nr.getOutputs()))
                .nodeProcessData(parseJson(nr.getProcessData()))
                .event(getNodeEventFromStatus(nr.getStatus()))
                .build();
    }

    private Object parseJson(String json) {
        return json != null ? JsonUtils.fromJson(json, Object.class) : null;
    }

    private String getNodeEventFromStatus(String status) {
        if(status == null)
            return "onWorkflowNodeRunStarted";
        switch (status) {
        case "succeeded":
            return "onWorkflowNodeRunSucceeded";
        case "failed":
            return "onWorkflowNodeRunFailed";
        case "waiting":
            return "onWorkflowNodeRunWaited";
        case "exception":
            return "onWorkflowNodeRunException";
        default:
            return "onWorkflowNodeRunStarted";
        }
    }

    private String getEventFromStatus(String status) {
        if(status == null)
            return "onWorkflowRunStarted";
        switch (status) {
        case "succeeded":
            return "onWorkflowRunSucceeded";
        case "failed":
            return "onWorkflowRunFailed";
        case "stopped":
            return "onWorkflowRunStopped";
        case "suspended":
            return "onWorkflowRunSuspended";
        case "resumed":
            return "onWorkflowRunResumed";
        default:
            return "onWorkflowRunStarted";
        }
    }

}
