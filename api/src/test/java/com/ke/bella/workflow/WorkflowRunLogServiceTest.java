package com.ke.bella.workflow;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.ke.bella.workflow.api.WorkflowOps;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.service.WorkflowRunCallback;
import com.ke.bella.workflow.service.WorkflowRunCallback.WorkflowRunLog;
import com.ke.bella.workflow.service.WorkflowRunLogService;

public class WorkflowRunLogServiceTest extends AbstractTest {

    @Autowired
    WorkflowRunLogService logService;

    @Test
    public void testPageWorkflowAggregationByBaseQuery() {
        String workflowId = "WKFL-30b29a30-140e-40da-9cfb-f6ccfe646ab1";
        String workflowRunID1 = "RUN-2503061657010207000139";
        String workflowRunID2 = "RUN-2503061656270207000138";
        WorkflowRunLogService.QueryOps ops1 = WorkflowRunLogService.QueryOps.builder()
                .workflowId(workflowId)
                .triggerFroms(Arrays.stream(WorkflowOps.TriggerFrom.values()).map(WorkflowOps.TriggerFrom::name).collect(Collectors.toList()))
                .events(WorkflowRunCallback.WorkflowRunEvent.runFinishedEvents().stream().map(Enum::name).collect(Collectors.toList()))
                .status(Lists.newArrayList(WorkflowRunState.WorkflowRunStatus.succeeded.name()))
                .workflowRunId(null)
                .userId(userIdL)
                .fromIndex(0)
                .size(7)
                .build();
        Page<WorkflowRunLog> logPage = logService.pageWorkflowRunLogs(ops1);
        Assertions.assertNotNull(logPage);
        Assertions.assertNotNull(logPage.getData());
        Assertions.assertTrue(logPage.getData().stream().anyMatch(log -> log.getWorkflowRunId().equals(workflowRunID1)));
        Assertions.assertTrue(logPage.getData().stream().anyMatch(log -> log.getWorkflowRunId().equals(workflowRunID2)));

        WorkflowRunLogService.QueryOps ops2 = WorkflowRunLogService.QueryOps.builder()
                .workflowId(workflowId)
                .triggerFroms(Arrays.stream(WorkflowOps.TriggerFrom.values()).map(WorkflowOps.TriggerFrom::name).collect(Collectors.toList()))
                .events(WorkflowRunCallback.WorkflowRunEvent.runFinishedEvents().stream().map(Enum::name).collect(Collectors.toList()))
                .status(Lists.newArrayList(WorkflowRunState.WorkflowRunStatus.succeeded.name()))
                .workflowRunId(workflowRunID1)
                .userId(userIdL)
                .fromIndex(0)
                .size(6)
                .build();

        Page<WorkflowRunLog> logPageWithRunId = logService.pageWorkflowRunLogs(ops2);
        Assertions.assertNotNull(logPageWithRunId);
        Assertions.assertNotNull(logPageWithRunId.getData());
        Assertions.assertTrue(logPageWithRunId.getData().stream().allMatch(log -> log.getWorkflowRunId().equals(workflowRunID1)));
        Assertions.assertEquals(1, logPageWithRunId.getTotal());
    }

}
