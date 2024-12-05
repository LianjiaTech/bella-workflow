package com.ke.bella.workflow.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.StringUtils;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.utils.OpenAiUtils;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.OpenAiService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowRunCallback extends WorkflowCallbackAdaptor {

    private final WorkflowService service;
    private final IWorkflowCallback delegate;
    private final OpenAiService openAiService;
    private final Map<String, StringBuilder> resultBufferMap;

    public WorkflowRunCallback(WorkflowService service, IWorkflowCallback delegate) {
        this.service = service;
        this.delegate = delegate;
        this.openAiService = OpenAiUtils.defaultOpenAiService(BellaContext.getApiKey());
        this.resultBufferMap = new ConcurrentHashMap<>();
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext ctx) {
        LOGGER.info("{} {} onWorkflowRunStarted", ctx.getWorkflowId(), ctx.getRunId());

        // 如果stateful打开且为chatflow，则视具体情况创建thread
        String threadId = ctx.getThreadId();
        if(ctx.isStateful() && "advanced-chat".equals(ctx.getWorkflowMode()) && !StringUtils.hasText(threadId)) {
            threadId = openAiService.createThread(new ThreadRequest()).getId();
            ctx.setThreadId(threadId);
        }

        service.markWorkflowRunStarted(ctx);

        delegate.onWorkflowRunStarted(ctx);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext ctx) {
        LOGGER.info("{} {} onWorkflowRunSucceeded", ctx.getWorkflowId(), ctx.getRunId());

        service.markWorkflowRunSuccessed(ctx, ctx.getWorkflowRunResult().getOutputs());

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
        LOGGER.info("{} {} onWorkflowRunSuspended", context.getWorkflowId(), context.getRunId());

        delegate.onWorkflowRunSuspended(context);

        service.dumpWorkflowRunState(context);
        service.updateWorkflowRunStatus(context, WorkflowRunStatus.suspended.name());
    }

    @Override
    public void onWorkflowRunStopped(WorkflowContext context) {
        LOGGER.info("{} {} onWorkflowRunStopped", context.getWorkflowId(), context.getRunId());

        service.updateWorkflowRunStatus(context, WorkflowRunStatus.stopped.name());

        delegate.onWorkflowRunStopped(context);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        LOGGER.info("{} {} onWorkflowRunResumed", context.getWorkflowId(), context.getRunId());

        service.updateWorkflowRunStatus(context, WorkflowRunStatus.running.name());

        delegate.onWorkflowRunResumed(context);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        LOGGER.warn("{} {} onWorkflowRunFailed, error: {}", context.getWorkflowId(), context.getRunId(), error, t);

        service.updateWorkflowRun(context, WorkflowRunStatus.failed.name(), error);

        delegate.onWorkflowRunFailed(context, error, t);
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
        LOGGER.info("{} {} onWorkflowNodeRunStarted", context.getWorkflowId(), context.getRunId());

        if(!context.isFlashMode()) {
            service.createWorkflowNodeRun(context, nodeId, nodeRunId, NodeRunResult.Status.running.name());
        }

        delegate.onWorkflowNodeRunStarted(context, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData data) {
        LOGGER.info("{} {} onWorkflowNodeRunProgress", context.getWorkflowId(), context.getRunId());
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
        LOGGER.info("{} {} onWorkflowNodeRunWaited", context.getWorkflowId(), context.getRunId());

        if(!context.isFlashMode()) {
            service.updateWorkflowNodeRunWaited(context, nodeId, nodeRunId);
        }

        delegate.onWorkflowNodeRunWaited(context, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext ctx, String nodeId, String nodeRunId) {
        LOGGER.info("{} {} onWorkflowNodeRunSucceeded, nodeId:{} result: {}", ctx.getWorkflowId(), ctx.getRunId(), nodeId,
                ctx.getState().getNodeState(nodeId).getOutputs());

        if(!ctx.isFlashMode()) {
            service.updateWorkflowNodeRunSucceeded(ctx, nodeId, nodeRunId);
        }

        delegate.onWorkflowNodeRunSucceeded(ctx, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
        LOGGER.info("{} {} onWorkflowNodeRunFailed. error:{}", context.getWorkflowId(), context.getRunId(), error, t);

        if(!context.isFlashMode()) {
            service.updateWorkflowNodeRunFailed(context, nodeId, nodeRunId, error);
        }

        delegate.onWorkflowNodeRunFailed(context, nodeId, nodeRunId, error, t);
    }

    @Override
    public void onWorkflowIterationStarted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        LOGGER.info("{} {} onWorkflowIterationStarted, index: {}", context.getWorkflowId(), context.getRunId(), index);
        delegate.onWorkflowIterationStarted(context, nodeId, nodeRunId, index);
    }

    @Override
    public void onWorkflowIterationCompleted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        LOGGER.info("{} {} onWorkflowIterationCompleted, index: {}", context.getWorkflowId(), context.getRunId(), index);
        delegate.onWorkflowIterationCompleted(context, nodeId, nodeRunId, index);
    }
}
