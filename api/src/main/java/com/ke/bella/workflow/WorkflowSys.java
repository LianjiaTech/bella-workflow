package com.ke.bella.workflow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.ke.bella.workflow.IWorkflowCallback.Delta;
import com.ke.bella.workflow.IWorkflowCallback.ProgressData;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.node.BaseNode;
import com.ke.bella.workflow.service.code.Requests;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.OpenAiUtils;
import com.ke.bella.workflow.utils.Utils;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.service.OpenAiService;

import io.reactivex.Flowable;
import lombok.Builder;

@SuppressWarnings("serial")
@Builder
public class WorkflowSys extends LinkedHashMap<String, Object> {

    transient WorkflowContext context;
    transient IWorkflowCallback callback;

    @Builder.Default
    transient Requests http = new Requests();

    @Override
    public Object get(Object key) {
        return context.getState().getVariable("sys", key.toString());
    }

    @Override
    public synchronized Object put(String key, Object value) {
        Object old = get(key);
        context.getState().putVariable("sys", key, value);
        return old;
    }

    public Requests http() {
        if(http == null) {
            http = new Requests();
        }
        return http;
    }

    public Object getVariable(String nodeId, String key) {
        return context.getState().getVariable(nodeId, key);
    }

    public String getCallbackUrl(BaseNode<?> node) {
        return node.getCallbackUrl(context.getTenantId(), context.getWorkflowId(), context.getRunId());
    }

    public String nodeId(String name) {
        com.ke.bella.workflow.WorkflowSchema.Node node = context.getGraph().getMeta()
                .getGraph().getNodes().stream()
                .filter(n -> n.getTitle().equals(name)).findFirst().get();
        return node.getId();
    }

    public long timestamp() {
        return System.currentTimeMillis();
    }

    public String uuid() {
        return UUID.randomUUID().toString();
    }

    public String newMessageId() {
        return IDGenerator.newMessageId();
    }

    public void sleep(long timeout) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(Math.max(timeout, 1));
    }

    private String flowKey(String key) {
        return String.format("%s-%s-%s", context.getTenantId(), context.getWorkflowId(), key);
    }

    public void onProgress(BaseNode<?> self, Object data) {
        ProgressData progress = ProgressData.builder()
                .data(data)
                .build();
        callback.onWorkflowNodeRunProgress(context, self.getNodeId(), self.getNodeRunId(), progress);
    }

    public void onProgress(BaseNode<?> self, String messageId, String text) {
        Delta delta = Delta.builder()
                .name(self.getNodeData().getMessageRoleName())
                .content(Delta.fromText(text))
                .messageId(messageId)
                .build();

        ProgressData progress = ProgressData.builder()
                .object(ProgressData.ObjectType.DELTA_CONTENT)
                .data(delta)
                .build();
        callback.onWorkflowNodeRunProgress(context, self.getNodeId(), self.getNodeRunId(), progress);
    }

    @SuppressWarnings("rawtypes")
    public Object chat(Object req) {
        ChatCompletionRequest request = null;
        if(req instanceof ChatCompletionRequest) {
            request = (ChatCompletionRequest) req;
        } else if(req instanceof Map) {
            request = JsonUtils.convertValue((Map) req, ChatCompletionRequest.class);
        } else {
            throw new IllegalArgumentException("arg's type should be ChatCompletionRequest or map.");
        }

        OpenAiService service = OpenAiUtils.defaultOpenAiService(BellaContext.getApiKey(), 30, TimeUnit.SECONDS);
        if(request.getStream() != null && request.getStream().booleanValue()) {
            Flowable<ChatCompletionChunk> rs = service.streamChatCompletion(request);
            return rs.blockingIterable();
        } else {
            return service.createChatCompletion(request);
        }
    }

    public Object fromJson(String json) {
        return JsonUtils.fromJson(json, Object.class);
    }

    public static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    public static long getMemoryUsage() {
        return Utils.getThreadAllocatedBytes(Thread.currentThread().getId());
    }
}
