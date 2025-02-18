package com.ke.bella.workflow.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.base.Throwables;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.OpenAiUtils;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.OpenAiService;

import lombok.Data;
import lombok.NoArgsConstructor;

public class WorkflowRunCallback extends WorkflowCallbackAdaptor {
    private static final Logger WORKFLOW_RUN_LOGGER = LoggerFactory.getLogger("workflowRun");
    private final WorkflowService service;
    private final IWorkflowCallback delegate;
    private final OpenAiService openAiService;
    private final Map<String, StringBuilder> resultBufferMap;

    public WorkflowRunCallback(WorkflowService service, IWorkflowCallback delegate) {
        this.service = service;
        this.delegate = delegate;
        this.openAiService = OpenAiUtils.defaultOpenAiService(BellaContext.getApikey().getApikey());
        this.resultBufferMap = new ConcurrentHashMap<>();
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext ctx) {
        // 如果stateful打开且为chatflow，则视具体情况创建thread
        String threadId = ctx.getThreadId();
        if(ctx.isStateful() && "advanced-chat".equals(ctx.getWorkflowMode()) && !StringUtils.hasText(threadId)) {
            threadId = openAiService.createThread(new ThreadRequest()).getId();
            ctx.setThreadId(threadId);
        }

        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowRunStarted");
        runLog.setTenantId(ctx.getTenantId());
        runLog.setUserId(BellaContext.getOperator().getUserId());
        runLog.setUserName(BellaContext.getOperator().getUserName());
        runLog.setWorkflowId(ctx.getWorkflowId());
        runLog.setWorkflowRunId(ctx.getRunId());
        runLog.setFlashMode(ctx.getFlashMode());
        runLog.setTriggerFrom(ctx.getTriggerFrom());
        runLog.setStateful(ctx.isStateful());
        runLog.setSys(ctx.getState().getVariablePool().get("sys"));
        runLog.setInputs(ctx.userInputs());
        runLog.setThreadId(ctx.getThreadId());
        runLog.setStateful(ctx.isStateful());
        runLog.setStatus(WorkflowRunStatus.running.name());
        runLog.setCtime(System.currentTimeMillis());

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        service.markWorkflowRunStarted(ctx);

        delegate.onWorkflowRunStarted(ctx);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowRunSucceeded");
        runLog.setTenantId(ctx.getTenantId());
        runLog.setUserId(BellaContext.getOperator().getUserId());
        runLog.setUserName(BellaContext.getOperator().getUserName());
        runLog.setWorkflowId(ctx.getWorkflowId());
        runLog.setWorkflowRunId(ctx.getRunId());
        runLog.setOutputs(Optional.ofNullable(ctx.getWorkflowRunResult()).map(NodeRunResult::getOutputs).orElse(null));
        runLog.setThreadId(ctx.getThreadId());
        runLog.setStateful(ctx.isStateful());
        runLog.setStatus(WorkflowRunStatus.succeeded.name());
        runLog.setElapsedTime(ctx.elapsedTime(now));
        runLog.setCtime(System.currentTimeMillis());
        runLog.setTriggerFrom(ctx.getTriggerFrom());

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        service.markWorkflowRunSuccessed(ctx, Optional.ofNullable(ctx.getWorkflowRunResult()).map(NodeRunResult::getOutputs).orElse(null));

        delegate.onWorkflowRunSucceeded(ctx);

        if(ctx.isStateful() && "advanced-chat".equals(ctx.getWorkflowMode())) {
            openAiService.createMessage(ctx.getThreadId(),
                    new MessageRequest("user", ctx.getState().getVariable("sys", "query"), null, null));
            for (StringBuilder buffer : resultBufferMap.values()) {
                openAiService.createMessage(ctx.getThreadId(), new MessageRequest("assistant", buffer.toString(), null, null));
            }
        }
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowRunSuspended");
        runLog.setTenantId(context.getTenantId());
        runLog.setUserId(BellaContext.getOperator().getUserId());
        runLog.setUserName(BellaContext.getOperator().getUserName());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());
        runLog.setThreadId(context.getThreadId());
        runLog.setStateful(context.isStateful());
        runLog.setStatus(WorkflowRunStatus.running.name());
        runLog.setElapsedTime(context.elapsedTime(LocalDateTime.now()));
        runLog.setCtime(System.currentTimeMillis());

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        delegate.onWorkflowRunSuspended(context);

        service.dumpWorkflowRunState(context);
        service.updateWorkflowRunStatus(context, WorkflowRunStatus.suspended.name());
    }

    @Override
    public void onWorkflowRunStopped(WorkflowContext context) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowRunStopped");
        runLog.setTenantId(context.getTenantId());
        runLog.setUserId(BellaContext.getOperator().getUserId());
        runLog.setUserName(BellaContext.getOperator().getUserName());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());
        runLog.setThreadId(context.getThreadId());
        runLog.setStateful(context.isStateful());
        runLog.setStatus(WorkflowRunStatus.suspended.name());
        runLog.setElapsedTime(context.elapsedTime(LocalDateTime.now()));
        runLog.setCtime(System.currentTimeMillis());
        runLog.setTriggerFrom(context.getTriggerFrom());

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        service.updateWorkflowRunStatus(context, WorkflowRunStatus.stopped.name());

        delegate.onWorkflowRunStopped(context);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowRunResumed");
        runLog.setTenantId(context.getTenantId());
        runLog.setUserId(BellaContext.getOperator().getUserId());
        runLog.setUserName(BellaContext.getOperator().getUserName());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());
        runLog.setThreadId(context.getThreadId());
        runLog.setStateful(context.isStateful());
        runLog.setStatus(WorkflowRunStatus.stopped.name());
        runLog.setElapsedTime(context.elapsedTime(LocalDateTime.now()));
        runLog.setCtime(System.currentTimeMillis());

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        service.updateWorkflowRunStatus(context, WorkflowRunStatus.running.name());

        delegate.onWorkflowRunResumed(context);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowRunFailed");
        runLog.setTenantId(context.getTenantId());
        runLog.setUserId(BellaContext.getOperator().getUserId());
        runLog.setUserName(BellaContext.getOperator().getUserName());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());
        runLog.setError(Throwables.getStackTraceAsString(t));
        runLog.setThreadId(context.getThreadId());
        runLog.setStateful(context.isStateful());
        runLog.setStatus(WorkflowRunStatus.failed.name());
        runLog.setElapsedTime(context.elapsedTime(LocalDateTime.now()));
        runLog.setCtime(System.currentTimeMillis());
        runLog.setTriggerFrom(context.getTriggerFrom());

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        service.updateWorkflowRun(context, WorkflowRunStatus.failed.name(), t.toString());

        delegate.onWorkflowRunFailed(context, error, t);
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowNodeRunStarted");
        runLog.setTenantId(context.getTenantId());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());

        runLog.setNodeId(nodeId);
        runLog.setNodeRunId(nodeRunId);
        runLog.setNodeTitle(context.getGraph().node(nodeId).getTitle());
        runLog.setNodeType(context.getGraph().node(nodeId).getNodeType());
        runLog.setCtime(System.currentTimeMillis());

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        if(!context.isFlashMode()) {
            service.createWorkflowNodeRun(context, nodeId, nodeRunId, NodeRunResult.Status.running.name());
        }

        delegate.onWorkflowNodeRunStarted(context, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData data) {

        delegate.onWorkflowNodeRunProgress(context, nodeId, nodeRunId, data);

        if(context.isStateful() && "advanced-chat".equals(context.getWorkflowMode())) {
            if(ProgressData.ObjectType.DELTA_CONTENT.equals(data.getObject())) {
                if(!resultBufferMap.containsKey(((Delta) data.getData()).getMessageId())) {
                    resultBufferMap.put(((Delta) data.getData()).getMessageId(), new StringBuilder());
                }
                resultBufferMap.get(((Delta) data.getData()).getMessageId()).append(data.getData().toString());
            }
        }
    }

    @Override
    public void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId, String nodeRunId) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowNodeRunWaited");
        runLog.setTenantId(context.getTenantId());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());

        runLog.setNodeId(nodeId);
        runLog.setNodeRunId(nodeRunId);
        runLog.setNodeTitle(context.getGraph().node(nodeId).getTitle());
        runLog.setNodeType(context.getGraph().node(nodeId).getNodeType());
        runLog.setCtime(System.currentTimeMillis());
        if(context.getState().getNodeState(nodeId) != null) {
            runLog.setNodeInputs(context.getState().getNodeState(nodeId).getInputs());
            runLog.setNodeProcessData(context.getState().getNodeState(nodeId).getProcessData());
        }

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        if(!context.isFlashMode()) {
            service.updateWorkflowNodeRunWaited(context, nodeId, nodeRunId);
        }

        delegate.onWorkflowNodeRunWaited(context, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext ctx, String nodeId, String nodeRunId) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowNodeRunSucceeded");
        runLog.setTenantId(ctx.getTenantId());
        runLog.setWorkflowId(ctx.getWorkflowId());
        runLog.setWorkflowRunId(ctx.getRunId());

        runLog.setNodeId(nodeId);
        runLog.setNodeRunId(nodeRunId);
        runLog.setNodeTitle(ctx.getGraph().node(nodeId).getTitle());
        runLog.setNodeType(ctx.getGraph().node(nodeId).getNodeType());
        runLog.setCtime(System.currentTimeMillis());
        if(ctx.getState().getNodeState(nodeId) != null) {
            runLog.setNodeInputs(ctx.getState().getNodeState(nodeId).getInputs());
            runLog.setNodeProcessData(ctx.getState().getNodeState(nodeId).getProcessData());
            runLog.setNodeOutputs(ctx.getState().getNodeState(nodeId).getOutputs());
        }

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        if(!ctx.isFlashMode()) {
            service.updateWorkflowNodeRunSucceeded(ctx, nodeId, nodeRunId);
        }

        delegate.onWorkflowNodeRunSucceeded(ctx, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowNodeRunFailed");
        runLog.setTenantId(context.getTenantId());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());
        runLog.setError(Throwables.getStackTraceAsString(t));

        runLog.setNodeId(nodeId);
        runLog.setNodeRunId(nodeRunId);
        runLog.setNodeTitle(context.getGraph().node(nodeId).getTitle());
        runLog.setNodeType(context.getGraph().node(nodeId).getNodeType());
        runLog.setCtime(System.currentTimeMillis());
        if(context.getState().getNodeState(nodeId) != null) {
            runLog.setNodeInputs(context.getState().getNodeState(nodeId).getInputs());
            runLog.setNodeProcessData(context.getState().getNodeState(nodeId).getProcessData());
        }

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        if(!context.isFlashMode()) {
            service.updateWorkflowNodeRunFailed(context, nodeId, nodeRunId, error);
        }

        delegate.onWorkflowNodeRunFailed(context, nodeId, nodeRunId, error, t);
    }

    @Override
    public void onWorkflowIterationStarted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowIterationStarted");
        runLog.setTenantId(context.getTenantId());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());
        runLog.setCtime(System.currentTimeMillis());

        runLog.setNodeId(nodeId);
        runLog.setNodeRunId(nodeRunId);

        runLog.setIteration(true);
        runLog.setIterationIndex(index);

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        delegate.onWorkflowIterationStarted(context, nodeId, nodeRunId, index);
    }

    @Override
    public void onWorkflowIterationCompleted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        WorkflowRunLog runLog = new WorkflowRunLog();
        runLog.setBellaTraceId(BellaContext.getTraceId());
        runLog.setAkCode(BellaContext.getAkCode());
        runLog.setEvent("onWorkflowIterationCompleted");
        runLog.setTenantId(context.getTenantId());
        runLog.setWorkflowId(context.getWorkflowId());
        runLog.setWorkflowRunId(context.getRunId());
        runLog.setCtime(System.currentTimeMillis());

        runLog.setNodeId(nodeId);
        runLog.setNodeRunId(nodeRunId);
        if(context.getState().getNodeState(nodeId) != null) {
            runLog.setNodeInputs(context.getState().getNodeState(nodeId).getInputs());
            runLog.setNodeProcessData(context.getState().getNodeState(nodeId).getProcessData());
            runLog.setNodeOutputs(context.getState().getNodeState(nodeId).getOutputs());
        }

        runLog.setIteration(true);
        runLog.setIterationIndex(index);

        WORKFLOW_RUN_LOGGER.info(JsonUtils.toJson(runLog));

        delegate.onWorkflowIterationCompleted(context, nodeId, nodeRunId, index);
    }

    @Data
    @NoArgsConstructor
    public static class WorkflowRunLog {
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
        private Object sys;
        private Object inputs;
        private Object outputs;
        private String status;
        private Long ctime;
        private Long elapsedTime;
        private String nodeId;
        private String nodeType;
        private String nodeTitle;
        private String nodeRunId;
        private Object nodeInputs;
        private Object nodeProcessData;
        private Object nodeOutputs;
        private String error;
        private boolean iteration;
        private Integer iterationIndex;
    }
}
