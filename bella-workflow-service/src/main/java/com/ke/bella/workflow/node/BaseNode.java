package com.ke.bella.workflow.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema;

import lombok.Data;

@Data
class BaseNodeData {
    private String title;
    private String desc;
    private String type;
}

public abstract class BaseNode implements RunnableNode {
    private static final Map<String, Class<? extends BaseNode>> NODE_RUNNER_CLASSES = new ConcurrentHashMap<>();
    static {
        register(NodeType.START.name, Start.class);
        register(NodeType.END.name, End.class);
    }

    protected WorkflowSchema.Node meta;

    protected BaseNode(WorkflowSchema.Node meta) {
        this.meta = meta;
    }

    public abstract NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback);

    @Override
    public NodeRunResult run(WorkflowContext context, IWorkflowCallback callback) {
        callback.onWorkflowNodeRunStarted(context, meta.getId());
        NodeRunResult result = execute(context, callback);
        context.putNodeRunResult(meta.getId(), result);

        if(result.getStatus() == NodeRunResult.Status.succeeded) {
            callback.onWorkflowNodeRunSucceeded(context, meta.getId());
        } else if(result.getStatus() == NodeRunResult.Status.failed) {
            callback.onWorkflowNodeRunFailed(context, meta.getId(), result.getError().getMessage(), result.getError());
        }
        return result;
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
