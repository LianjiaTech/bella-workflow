package com.ke.bella.workflow.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.service.WorkflowRunCallback.WorkflowRunLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

public interface IWorkflowRunLogService {

    WorkflowRunLog getWorkflowRunLog(String workflowRunId);

    Page<WorkflowRunLog> pageWorkflowRunLogs(QueryOps ops);

    void saveWorkflowRunLog(WorkflowRunLog runLog);

    Map<String, List<Map<String, Object>>> getDailyRunsStatistic(String workflowId, LocalDateTime startDate, LocalDateTime endDate);

    @Data
    @Builder
    class QueryOps {
        private String workflowId;
        private List<String> triggerFroms;
        private List<String> status;
        private String workflowRunId;
        private List<String> events;
        private Long userId;
        @Builder.Default
        private Integer fromIndex = 0;
        @Builder.Default
        private Integer size = 1000;
        @Builder.Default
        private String orderBy = "ctime";
        @Builder.Default
        private String order = "desc";
        private String lastWorkflowRunId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class WorkflowDailyStatistic {
        private String date;
        @Builder.Default
        private Long runs = 0L;
        @Builder.Default
        private Long terminalCount = 0L;
        @Builder.Default
        private BigDecimal interactions = BigDecimal.ZERO;
    }
}
