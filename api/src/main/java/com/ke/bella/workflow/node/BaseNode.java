package com.ke.bella.workflow.node;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.IWorkflowCallback.ProgressData;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowNodeRunException;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.workflow.service.Configs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseNode<T extends BaseNode.BaseNodeData> implements RunnableNode {
    @SuppressWarnings("rawtypes")
    private static final Map<String, Class<? extends BaseNode>> NODE_RUNNER_CLASSES = new ConcurrentHashMap<>();
    static {
        register(NodeType.START.name, Start.class);
        register(NodeType.END.name, End.class);
        register(NodeType.IF_ELSE.name, IfElseNode.class);
        register(NodeType.HTTP_REQUEST.name, HttpNode.class);
        register(NodeType.TEMPLATE_TRANSFORM.name, TemplateTransformNode.class);
        register(NodeType.QUESTION_CLASSIFIER.name, QuestionClassifierNode.class);
        register(NodeType.KNOWLEDGE_RETRIEVAL.name, KnowledgeRetrievalNode.class);
        register(NodeType.LLM.name, LlmNode.class);
        register(NodeType.ITERATION.name, Iteration.class);
        register(NodeType.TOOL.name, ToolNode.class);
        register(NodeType.PARAMETER_EXTRACTOR.name, ParameterExtractorNode.class);
        register(NodeType.CODE.name, CodeNode.class);
        register(NodeType.PARALLEL.name, ParallelNode.class);
        register(NodeType.RAG.name, RagNode.class);
    }

    protected WorkflowSchema.Node meta;
    @Getter
    @Setter
    protected String nodeRunId;
    protected T data;
    @Getter
    protected ResumeData resumeData;

    @Getter
    private PrintStream out;

    @Getter
    private String callbackUrl;

    protected BaseNode(WorkflowSchema.Node meta, T data) {
        this(meta, UUID.randomUUID().toString(), data);
    }

    protected BaseNode(WorkflowSchema.Node meta, String nodeRunId, T data) {
        this.meta = meta;
        this.nodeRunId = nodeRunId;
        this.data = data;
    }

    protected void beforeExecute(WorkflowContext context) {
        // no-op
    }

    protected abstract NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback);

    @SuppressWarnings("rawtypes")
    protected NodeRunResult resume(WorkflowContext context, IWorkflowCallback callback, Map notifyData) {
        NodeRunResult r = context.getState().getNodeState(getNodeId());
        return NodeRunResult.builder()
                .inputs(r.getInputs())
                .processData(r.getProcessData())
                .outputs(notifyData)
                .status(NodeRunResult.Status.succeeded)
                .build();
    }

    protected void afterExecute(WorkflowContext context) {
        // no-op
    }

    public T getNodeData() {
        return data;
    }

    public String getNodeId() {
        return this.meta.getId();
    }

    public WorkflowSchema.Node getMeta() {
        return meta;
    }

    protected boolean isCallback() {
        return data.isWaitCallback();
    }

    public boolean isResuming() {
        return resumeData != null;
    }

    @Override
    public NodeRunResult run(WorkflowContext context, IWorkflowCallback callback) {
        if(context.isInterrupted()) {
            return NodeRunResult.newSkippedResult(null);
        }

        Long startTime = System.nanoTime();

        setOut(context, callback);

        appendBuiltinVariables(context);

        if("DEBUG_NODE".equals(context.getTriggerFrom())) {
            appendUserInputsAsVariables(context);
        }

        beforeExecute(context);
        callback.onWorkflowNodeRunStarted(context, meta.getId(), nodeRunId);
        NodeRunResult result = execute(context, callback);
        try {
            result.setElapsedTime((System.nanoTime() - startTime) / 1000000L);

            if(result.getStatus() == NodeRunResult.Status.succeeded) {
                List<String> handles = data.getSourceHandles();
                if(handles.size() == 1) {
                    result.setActivatedSourceHandles(handles);
                }
            }

            synchronized(context) {
                context.putNodeRunResult(this, result);
                if(result.getStatus() == NodeRunResult.Status.succeeded) {
                    callback.onWorkflowNodeRunSucceeded(context, meta.getId(), nodeRunId);
                } else if(result.getStatus() == NodeRunResult.Status.failed) {
                    WorkflowNodeRunException e = WorkflowNodeRunException.from(this, result.getError());
                    callback.onWorkflowNodeRunFailed(context, meta.getId(), nodeRunId, e.getMessage(), e);
                    throw e;
                } else if(result.getStatus() == NodeRunResult.Status.waiting) {
                    callback.onWorkflowNodeRunWaited(context, meta.getId(), nodeRunId);
                }
            }

        } finally {
            afterExecute(context);
            LOGGER.debug("[{}]-{}-node execution result: {}", context.getRunId(), meta.getId(), result);
        }

        return result;
    }

    @SuppressWarnings({ "unchecked" })
    private void appendUserInputsAsVariables(WorkflowContext context) {
        final Map<String, Object> userInputs = context.userInputs();
        userInputs.forEach((k, v) -> {
            if(k.startsWith("#") && k.endsWith("#")) {
                String[] selector = k.substring(1, k.length() - 1).split("\\.");
                context.getState().putVariableValue(Arrays.asList(selector), userInputs.get(k));
            } else {
                throw new IllegalArgumentException("用户输入的变量名不合法，需要是`#key1.key2#`格式");
            }
        });
    }

    public NodeRunResult resume(WorkflowContext context, IWorkflowCallback callback) {
        NodeRunResult result = null;
        try {
            setOut(context, callback);
            this.resumeData = ResumeData.builder()
                    .notifyData(context.getState().getNotifyData(getNodeId()))
                    .lastState(context.getState().getNodeState(getNodeId()))
                    .build();
            result = resume(context, callback, context.getState().getNotifyData(getNodeId()));

            if(result.getStatus() == NodeRunResult.Status.succeeded) {
                List<String> handles = data.getSourceHandles();
                if(handles.size() == 1) {
                    result.setActivatedSourceHandles(handles);
                }
            }

            synchronized(context) {
                context.putNodeRunResult(this, result);
                if(result.getStatus() == NodeRunResult.Status.succeeded) {
                    callback.onWorkflowNodeRunSucceeded(context, meta.getId(), nodeRunId);
                } else if(result.getStatus() == NodeRunResult.Status.failed) {
                    callback.onWorkflowNodeRunFailed(context, meta.getId(), nodeRunId, result.getError().toString(), result.getError());
                    throw new IllegalStateException(result.getError().getMessage());
                } else if(result.getStatus() == NodeRunResult.Status.waiting) {
                    callback.onWorkflowNodeRunWaited(context, meta.getId(), nodeRunId);
                }
            }

        } finally {
            afterExecute(context);
            this.resumeData = null;
            LOGGER.debug("[{}]-{}-node execution result: {}", context.getRunId(), meta.getId(), result);
        }
        return result;
    }

    public void validate(WorkflowContext ctx) {
        // no-op
    }

    private void appendBuiltinVariables(WorkflowContext context) {
        callbackUrl = getCallbackUrl(
                context.getTenantId(),
                context.getWorkflowId(),
                context.getRunId());

        // append callbackUrl
        if(isCallback()) {
            context.getState().putVariable(getNodeId(), "callbackUrl", callbackUrl);
        }
    }


    public String getCallbackUrl(String tenantId, String workflowId, String runId) {
        return String.format("%s/workflow/callback/%s/%s/%s/%s/%s",
                Configs.API_BASE,
                tenantId,
                workflowId,
                runId, getNodeId(), nodeRunId);
    }

    private void setOut(WorkflowContext context, IWorkflowCallback callback) {
        this.out = new PrintStream(ProgressOutStream.builder()
                .self(this)
                .context(context)
                .callback(callback).build(), true);
    }

    @SuppressWarnings("rawtypes")
    public static void register(String nodeType, Class<? extends BaseNode> clazz) {
        NODE_RUNNER_CLASSES.put(nodeType, clazz);
    }

    @SuppressWarnings("rawtypes")
    public static BaseNode from(WorkflowSchema.Node meta) {
        String type = meta.getNodeType();
        Class<? extends BaseNode> clazz = NODE_RUNNER_CLASSES.get(type);
        if(clazz == null) {
            throw new IllegalArgumentException(String.format("不支持的节点类型: %s", type));
        }
        try {
            return clazz.getConstructor(WorkflowSchema.Node.class).newInstance(meta);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    public static BaseNode from(WorkflowSchema.Node meta, String nodeRunId) {
        BaseNode node = from(meta);
        node.setNodeRunId(nodeRunId);
        return node;
    }

    @SuppressWarnings("rawtypes")
    public static List<BaseNode> from(List<WorkflowSchema.Node> metas) {
        List<BaseNode> ret = new ArrayList<>();
        for (WorkflowSchema.Node meta : metas) {
            ret.add(from(meta));
        }
        return ret;
    }

    @SuppressWarnings("rawtypes")
    public static List<BaseNode> from(WorkflowSchema.Node... metas) {
        return from(Arrays.asList(metas));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<Map<String, Object>> defaultConfigs() {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Class<? extends BaseNode> nodeClass : NODE_RUNNER_CLASSES.values()) {
                Map<String, Object> defaultConfig = (Map<String, Object>) nodeClass.getMethod("defaultConfig", Map.class).invoke(null,
                        Collections.emptyMap());
                if(!CollectionUtils.isEmpty(defaultConfig)) {
                    result.add(defaultConfig);
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> defaultConfigs(NodeType type, Map<String, Object> filters) {
        try {
            return (Map<String, Object>) NODE_RUNNER_CLASSES.get(type.name).getMethod("defaultConfig", Map.class).invoke(null, filters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> defaultConfig(Map<String, Object> filters) {
        return Collections.emptyMap();
    }

    @Data
    public static class BaseNodeData {
        private String title;
        private String desc;
        private String type;
        private boolean generateDeltaContent = false;
        private boolean generateNewMessage = false;
        private String messageRoleName;
        private boolean waitCallback = false;

        public List<String> getSourceHandles() {
            return Arrays.asList("source");
        }

        @lombok.Getter
        @lombok.Setter
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class Authorization {
            String apiKey;
            String apiBaseUrl;

            public String getToken() {
                if(apiKey == null) {
                    apiKey = BellaContext.getApikey().getApikey();
                }
                return String.format("Bearer %s", apiKey);
            }

            public String getApiBaseUrl() {
                return StringUtils.isEmpty(apiBaseUrl) ? Configs.OPEN_API_BASE : apiBaseUrl;
            }
        }

        @lombok.Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Model {
            private String provider;
            private String name;
            private String mode;
            @JsonAlias("completion_params")
            private Map<String, Object> completionParams;
        }
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ResumeData {
        NodeRunResult lastState;
        @SuppressWarnings("rawtypes")
        Map notifyData;
    }

    @Builder
    public static class ProgressOutStream extends OutputStream {
        @Builder.Default
        StringBuilder sb = new StringBuilder();
        WorkflowContext context;
        IWorkflowCallback callback;
        BaseNode<?> self;

        @Override
        public void write(int b) throws IOException {
            sb.append((char) b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            sb.append(new String(b, off, len));
        }

        @Override
        public void flush() throws IOException {
            String text = sb.toString();
            if(StringUtils.hasText(text) && text.endsWith("\n")) {
                callback.onWorkflowNodeRunProgress(context, self.getNodeId(), self.nodeRunId, ProgressData.log(text));
                sb.delete(0, sb.length());
            }
        }
    }
}
