package com.ke.bella.workflow.service;

import java.util.List;

import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.service.WorkflowRunCallback.WorkflowRunLog;

import lombok.Builder;
import lombok.Data;

public interface IWorkflowRunLogService {

    WorkflowRunLog getWorkflowRunLog(String workflowRunId);

    Page<WorkflowRunLog> pageWorkflowRunLogs(QueryOps ops);

    void saveWorkflowRunLog(WorkflowRunLog runLog);

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
    }
}
