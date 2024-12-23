package com.ke.bella.workflow.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.WorkflowGraph;
import com.ke.bella.workflow.WorkflowRunState;
import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunner;
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.node.BaseNode.BaseNodeData;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("rawtypes")
public class Iteration extends BaseNode<Iteration.Data> {

    public Iteration(Node meta) {
        super(meta, JsonUtils.convertValue(meta.getData(), Data.class));
    }

    @Override
    protected void beforeExecute(WorkflowContext context) {
        Object iter = context.getState().getVariableValue(data.getIteratorSelector());
        Assert.isTrue(iter instanceof List, "迭代节点的输入必须是List");

        List items = (List) iter;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("iterator_length", items.size());
        context.getState().putVariable(getNodeId(), "metadata", metadata);
    }

    @Override
    protected NodeRunResult execute(WorkflowContext context, IWorkflowCallback callback) {
        Map<String, Object> nodeInputs = new HashMap<>();
        Map<String, Object> processData = new HashMap<>();
        Map<String, Object> nodeOutputs = new HashMap<>();

        try {
            Object iter = context.getState().getVariableValue(data.getIteratorSelector());
            Assert.isTrue(iter instanceof List, "迭代节点的输入必须是List");

            List items = (List) iter;
            List<Object> output = new ArrayList<>();
            List<List> steps = new ArrayList<>();
            nodeInputs.put("iterator_selector", items);

            WorkflowSchema meta = context.getGraph().getMeta().iterationSchema(getNodeId());
            WorkflowGraph graph = new WorkflowGraph(meta, getNodeId());
            for (int i = 0; i < items.size(); i++) {
                IterationResult ir = iteration(graph, i, items.get(i), context, callback);
                output.add(ir.output);
                steps.add(ir.nodeRunIds);
            }
            nodeOutputs.put("output", output);
            processData.put("iterations", steps);
            return NodeRunResult.builder()
                    .inputs(nodeInputs)
                    .processData(processData)
                    .outputs(nodeOutputs)
                    .status(NodeRunResult.Status.succeeded)
                    .build();
        } catch (Exception e) {
            return NodeRunResult.builder()
                    .inputs(nodeInputs)
                    .processData(processData)
                    .outputs(nodeOutputs)
                    .status(NodeRunResult.Status.failed)
                    .error(e)
                    .build();
        }
    }

    private IterationResult iteration(WorkflowGraph graph, int index, Object item, WorkflowContext context, IWorkflowCallback callback) {
        WorkflowRunState state = new WorkflowRunState(context.getState().getVariablePool());

        state.putVariable(getNodeId(), "item", item);
        state.putVariable(getNodeId(), "index", index);

        IterationCallback cb = new IterationCallback(index, context, callback);
        WorkflowContext subContext = WorkflowContext.builder()
                .runId(context.getRunId())
                .graph(graph)
                .state(state)
                .userInputs(new HashMap())
                .triggerFrom(context.getTriggerFrom())
                .build();
        context.addChild(subContext);
        new WorkflowRunner().run(subContext, cb);

        return IterationResult.builder()
                .index(index)
                .output(state.getVariableValue(data.getOutputSelector()))
                .nodeRunIds(cb.nodeRunIds)
                .build();
    }

    @Builder
    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class IterationResult {
        int index;
        Object output;
        List<String> nodeRunIds;
    }

    class IterationCallback extends WorkflowCallbackAdaptor {
        final IWorkflowCallback parent;
        final WorkflowContext parentContext;
        final int index;
        List<String> nodeRunIds;

        public IterationCallback(int idx, WorkflowContext context, IWorkflowCallback callback) {
            this.index = idx;
            this.parent = callback;
            this.nodeRunIds = new ArrayList<>();
            this.parentContext = context;
        }

        @Override
        public void onWorkflowRunStarted(WorkflowContext context) {
            parent.onWorkflowIterationStarted(context, getNodeId(), nodeRunId, this.index);
        }

        @Override
        public void onWorkflowRunSucceeded(WorkflowContext context) {
            parent.onWorkflowIterationCompleted(parentContext, getNodeId(), nodeRunId, this.index);
        }

        @Override
        public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
            if(t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new IllegalStateException(error, t);
            }
        }

        @Override
        public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId) {
            parent.onWorkflowNodeRunStarted(context, nodeId, nodeRunId);
        }

        @Override
        public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData data) {
            parent.onWorkflowNodeRunProgress(context, nodeId, nodeRunId, data);
        }

        @Override
        public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId) {
            parent.onWorkflowNodeRunSucceeded(context, nodeId, nodeRunId);
            nodeRunIds.add(nodeRunId);
        }

        @Override
        public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t) {
            parent.onWorkflowNodeRunFailed(context, nodeId, nodeRunId, error, t);
            if(t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new IllegalStateException(error, t);
            }
        }

        @Override
        public void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId, String nodeRunId) {
            throw new IllegalArgumentException("迭代节点不支持挂起");
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data extends BaseNodeData {
        @JsonAlias({ "iterator_selector" })
        private List<String> iteratorSelector;

        @JsonAlias({ "output_selector" })
        private List<String> outputSelector;
    }
}
