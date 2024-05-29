package com.ke.bella.workflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Edge;
import com.ke.bella.workflow.node.BaseNode;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@SuppressWarnings("rawtypes")
public class WorkflowContext {
    private String tenantId;
    private String workflowId;
    private String runId;
    private String triggerFrom;
    private WorkflowGraph graph;
    private WorkflowRunState state;
    private Map userInputs;

    public Map userInputs() {
        return this.userInputs;
    }

    public void validate() {
        Assert.isTrue(graph != null, "工作流不能为null");
        Assert.isTrue(state != null, "工作流运行状态不能为null");
        Assert.isTrue(userInputs != null, "userInput不能为null");
    }

    public boolean isSuspended() {
        return !state.waitingNodeIds().isEmpty();
    }

    public BaseNode getNode(String nodeId) {
        return BaseNode.from(graph.node(nodeId));
    }

    public List<BaseNode> getNodes(List<String> nodeIds) {
        return nodeIds.stream()
                .map(id -> BaseNode.from(graph.node(id)))
                .collect(Collectors.toList());
    }

    public synchronized List<BaseNode> getNextNodes() {
        if(state.isEmpty()) {
            return Arrays.asList(BaseNode.from(graph.getStartNode()));
        } else {
            if(!state.waitingNodeIds().isEmpty()) {
                return Collections.emptyList();
            }

            // 找出所有未执行节点
            Set<String> ids = graph.nodeIds();
            ids.removeAll(state.completedNodeIds());

            // 只有当所有依赖节点都ready的时候才可以执行
            List<WorkflowSchema.Node> nodes = ids.stream()
                    .filter(id -> graph.inEdges(id).stream()
                            .allMatch(edge -> state.isActivated(edge.getSource(), edge.getSourceHandle())))
                    .map(graph::node)
                    .collect(Collectors.toList());

            return BaseNode.from(nodes);
        }
    }

    public synchronized void putNodeRunResult(String nodeId, NodeRunResult result) {
        // 针对只有一个后继节点的边集中处理激活标记
        // 这样，只有if，switch等多后继节点才需要单独在node runner中处理
        List<Edge> edges = this.graph.outEdges(nodeId);
        if(result.getError() == null && edges.size() == 1) {
            result.activatedSourceHandles = Arrays.asList(edges.get(0).getSourceHandle());
        }

        this.state.putNodeState(nodeId, result);
    }

    public void putWorkflowRunResult(NodeRunResult result) {
        this.state.setWorkflowRunResult(result);
    }

    public NodeRunResult getWorkflowRunResult() {
        return this.state.getWorkflowRunResult();
    }

    public boolean isResume(String nodeId) {
        return this.state.isResume(nodeId);
    }
}
