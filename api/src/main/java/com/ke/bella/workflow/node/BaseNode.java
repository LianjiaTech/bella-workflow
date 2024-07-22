package com.ke.bella.workflow.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.Configs;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
class BaseNodeData {
    private String title;
    private String desc;
    private String type;
    private boolean generateDeltaContent = false;

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
                apiKey = BellaContext.getApiKey();
            }
            return String.format("Bearer %s", apiKey);
        }

        public String getApiBaseUrl() {
            return StringUtils.isEmpty(apiBaseUrl) ? Configs.API_BASE : apiBaseUrl;
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

@Slf4j
public abstract class BaseNode implements RunnableNode {
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
    }

    protected WorkflowSchema.Node meta;
    protected String nodeRunId;

    protected BaseNode(WorkflowSchema.Node meta) {
        this(meta, UUID.randomUUID().toString());
    }

    protected BaseNode(WorkflowSchema.Node meta, String nodeRunId) {
        this.meta = meta;
        this.nodeRunId = nodeRunId;
    }

    protected abstract NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback);

    public String getNodeId() {
        return this.meta.getId();
    }

    public WorkflowSchema.Node getMeta() {
        return meta;
    }

    protected boolean isCallback() {
        return false;
    }

    @Override
    public NodeRunResult run(WorkflowContext context, IWorkflowCallback callback) {
        Long startTime = System.nanoTime();

        appendBuiltinVariables(context);

        if("DEBUG_NODE".equals(context.getTriggerFrom())) {
            appendUserInputsAsVariables(context);
        }

        callback.onWorkflowNodeRunStarted(context, meta.getId(), nodeRunId);

        NodeRunResult result = execute(context, callback);
        result.setElapsedTime((System.nanoTime() - startTime) / 1000000L);
        context.putNodeRunResult(meta.getId(), result);
        if(result.getStatus() == NodeRunResult.Status.succeeded) {
            callback.onWorkflowNodeRunSucceeded(context, meta.getId(), nodeRunId);
        } else if(result.getStatus() == NodeRunResult.Status.failed) {
            callback.onWorkflowNodeRunFailed(context, meta.getId(), nodeRunId, result.getError().getMessage(), result.getError());
        } else if(result.getStatus() == NodeRunResult.Status.waiting) {
            callback.onWorkflowNodeRunWaited(context, meta.getId(), nodeRunId);
        }

        LOGGER.debug("[{}]-{}-node execution result: {}", context.getRunId(), meta.getId(), result);

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
        NodeRunResult r = context.getState().getNodeState(getNodeId());
        return NodeRunResult.builder()
                .inputs(r.getInputs())
                .processData(r.getProcessData())
                .outputs(context.getState().getNotifyData(getNodeId()))
                .status(NodeRunResult.Status.succeeded)
                .build();
    }

    private void appendBuiltinVariables(WorkflowContext context) {
        // append callbackUrl
        if(isCallback()) {
            String url = String.format("%s/workflow/callback/%s/%s/%s/%s/%s ",
                    Configs.API_BASE,
                    context.getTenantId(),
                    context.getWorkflowId(),
                    context.getRunId(), getNodeId(), nodeRunId);
            context.getState().putVariable(getNodeId(), "callbackUrl", url);
        }
    }

    public static void register(String nodeType, Class<? extends BaseNode> clazz) {
        NODE_RUNNER_CLASSES.put(nodeType, clazz);
    }

    public static BaseNode from(WorkflowSchema.Node meta) {
        String type = meta.getType();
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

    public static List<BaseNode> from(List<WorkflowSchema.Node> metas) {
        List<BaseNode> ret = new ArrayList<>();
        for (WorkflowSchema.Node meta : metas) {
            ret.add(from(meta));
        }
        return ret;
    }

    public static List<BaseNode> from(WorkflowSchema.Node... metas) {
        return from(Arrays.asList(metas));
    }
}
