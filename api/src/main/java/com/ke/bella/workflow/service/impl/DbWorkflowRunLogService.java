package com.ke.bella.workflow.service.impl;

import java.util.List;
import java.util.stream.Collectors;

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
    public WorkflowRunLog getWorkflowRunLog(String workflowRunId) {
        WorkflowRunDB workflowRun = ws.getWorkflowRun(workflowRunId);
        if(workflowRun == null) {
            return null;
        }
        return transferToWorkflowRunLog(workflowRun);
    }

    @Override
    public Page<WorkflowRunLog> pageWorkflowRunLogs(QueryOps ops) {
        // Query workflow run logs
        WorkflowRunPage pageOps = WorkflowRunPage.builder()
                .workflowId(ops.getWorkflowId())
                .workflowRunId(ops.getWorkflowRunId())
                .triggerFroms(ops.getTriggerFroms())
                .userId(ops.getUserId())
                .status(ops.getStatus())
                .page(ops.getFromIndex() != null ? (ops.getFromIndex() / ops.getSize()) + 1 : 1)
                .pageSize(ops.getSize() != null ? ops.getSize() : 30)
                .lastId(ops.getLastWorkflowRunId())
                .build();

        Page<WorkflowRunDB> dbPage = ws.listWorkflowRun(pageOps);

        List<WorkflowRunLog> logList = dbPage.getData().stream()
                .map(this::transferToWorkflowRunLog)
                .collect(Collectors.toList());

        return Page.<WorkflowRunLog>from(dbPage.getPage(), dbPage.getPageSize())
                .total(dbPage.getTotal())
                .list(logList);
    }

    @Override
    public List<WorkflowRunLog> listNodeRunLogs(QueryOps ops) {
        return ws.getNodeRuns(ops.getWorkflowRunId()).stream()
                .map(this::transferNodeRunToWorkflowRunLog)
                .collect(Collectors.toList());
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
				.triggerId(wr.getTriggerId())
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
