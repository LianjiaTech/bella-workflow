package com.ke.bella.workflow.api.callbacks;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.Configs;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.service.OpenAiService;

public class DifyChatflowStreamingCallback extends WorkflowCallbackAdaptor {

    final DifyWorkflowRunStreamingCallback difyStreamingCallback;
    final StringBuilder resultBuffer;
    final OpenAiService openAiService;

    public DifyChatflowStreamingCallback(DifyWorkflowRunStreamingCallback difyStreamingCallback) {
        this.resultBuffer = new StringBuilder();
        this.difyStreamingCallback = difyStreamingCallback;
        this.openAiService = new OpenAiService(BellaContext.getApiKey(), Configs.API_BASE);
    }

    @Override
    public void onWorkflowRunStarted(WorkflowContext context) {
        difyStreamingCallback.onWorkflowRunStarted(context);
    }

    @Override
    public void onWorkflowRunSucceeded(WorkflowContext context) {
        difyStreamingCallback.onWorkflowRunSucceeded(context);
        // record messages only if chatflow run success
        openAiService.createMessage(context.getThreadId(), new MessageRequest("user", context.getState().getVariable("sys", "query"), null, null));
        openAiService.createMessage(context.getThreadId(), new MessageRequest("assistant", resultBuffer.toString(), null, null));
    }

    @Override
    public void onWorkflowRunSuspended(WorkflowContext context) {
        difyStreamingCallback.onWorkflowRunSuspended(context);
    }

    @Override
    public void onWorkflowRunResumed(WorkflowContext context) {
        difyStreamingCallback.onWorkflowRunResumed(context);
    }

    @Override
    public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
        difyStreamingCallback.onWorkflowRunFailed(context, error, t);
    }

    @Override
    public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
        difyStreamingCallback.onWorkflowNodeRunStarted(context, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData pdata) {
        difyStreamingCallback.onWorkflowNodeRunProgress(context, nodeId, nodeRunId, pdata);
        if(ProgressData.ObjectType.DELTA_CONTENT.equals(pdata.getObject())) {
            resultBuffer.append(pdata.getData().toString());
        }
    }

    @Override
    public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
        difyStreamingCallback.onWorkflowNodeRunSucceeded(context, nodeId, nodeRunId);
    }

    @Override
    public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
        difyStreamingCallback.onWorkflowNodeRunFailed(context, nodeId, nodeRunId, error, t);
    }

    @Override
    public void onWorkflowIterationStarted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        difyStreamingCallback.onWorkflowIterationStarted(context, nodeId, nodeRunId, index);
    }

    @Override
    public void onWorkflowIterationCompleted(WorkflowContext context, String nodeId, String nodeRunId, int index) {
        difyStreamingCallback.onWorkflowIterationCompleted(context, nodeId, nodeRunId, index);
    }

    public String getResult() {
        return resultBuffer.toString();
    }
}
