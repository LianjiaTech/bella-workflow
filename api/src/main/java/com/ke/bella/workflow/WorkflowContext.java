package com.ke.bella.workflow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowSchema.Edge;
import com.ke.bella.workflow.node.BaseNode;
import com.ke.bella.workflow.node.NodeType;
import com.ke.bella.workflow.node.Start;

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
    private LocalDateTime ctime;

    public Map userInputs() {
        return this.userInputs;
    }

    public void validate() {
        Assert.isTrue(graph != null, "工作流不能为null");
        Assert.isTrue(state != null, "工作流运行状态不能为null");
        Assert.isTrue(userInputs != null, "userInput不能为null");
        graph.validate();
        validateInputs();
    }

    private void validateInputs() {
        if(NodeType.START.name.equals(graph.getStartNode().getNodeType())) {
            Start start = (Start) BaseNode.from(graph.getStartNode());
            List<WorkflowSchema.VariableEntity> variables = start.getNodeData().getVariables();
            for (WorkflowSchema.VariableEntity variable : variables) {
                validate(variable, userInputs);
            }
        }
    }

    private void validate(WorkflowSchema.VariableEntity variable, Map inputs) {
        if(variable.isRequired() && !inputs.containsKey(variable.getVariable())) {
            throw new IllegalArgumentException(String.format("%s is required", variable.getVariable()));
        }
        if("object".equals(variable.getVarType())) {
            Object o = inputs.get(variable.getVariable());
            if(o == null) {
                return;
            }
            if(!CollectionUtils.isEmpty(variable.getChildren())) {
                if(!(o instanceof Map)) {
                    throw new IllegalArgumentException(String.format("invalid json input %s", variable.getVariable()));
                }
                for (WorkflowSchema.VariableEntity child : variable.getChildren()) {
                    validate(child, (Map) o);
                }
            }
        }
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

            // 把所有执行不到的node标记为跳过
            List<String> deadNodeIds = skipDeadNodes(ids);
            ids.removeAll(deadNodeIds);

            // 只有当所有依赖节点都ready的时候才可以执行
            List<WorkflowSchema.Node> nodes = ids.stream()
                    .filter(this::canNodeActive)
                    .map(graph::node)
                    .collect(Collectors.toList());

            return BaseNode.from(nodes);
        }
    }

    private List<String> skipDeadNodes(Set<String> ids) {
        List<String> dids = ids.stream()
                .filter(id -> graph.inEdges(id).stream()
                        .allMatch(
                                edge -> state.isDeadEdge(edge.getSource(), edge.getSourceHandle())))
                .collect(Collectors.toList());
        dids.forEach(id -> {
            BaseNode node = BaseNode.from(graph.node(id));
            List<String> handles = node.getNodeData().getSourceHandles();
            putNodeRunResult(node, NodeRunResult.newSkippedResult(handles));
        });
        return dids;
    }

    private boolean canNodeActive(String nodeId) {
        return graph.inEdges(nodeId).stream()
                .allMatch(edge -> state.isActivated(edge.getSource(), edge.getSourceHandle())
                        || state.isDeadEdge(edge.getSource(), edge.getSourceHandle()))
                && graph.inEdges(nodeId).stream()
                        .anyMatch(edge -> state.isActivated(edge.getSource(), edge.getSourceHandle()));

    }

    public synchronized void putNodeRunResult(BaseNode node, NodeRunResult result) {
        if(result.status == NodeRunResult.Status.succeeded) {
            // 针对只有一个后继节点的边集中处理激活标记
            // 这样，只有if，switch等多后继节点才需要单独在node runner中处理
            List<Edge> edges = this.graph.outEdges(node.getNodeId());
            if(result.getError() == null && edges.size() == 1) {
                result.activatedSourceHandles = Arrays.asList(edges.get(0).getSourceHandle());
            }

            List<String> handles = new ArrayList<>(node.getNodeData().getSourceHandles());
            handles.removeAll(result.getActivatedSourceHandles());
            result.setDeadSourceHandles(handles);
        }

        this.state.putNodeState(node.getNodeId(), result);
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

    public String getThreadId() {
        return this.getState().getVariable("sys", "thread_id") == null ? null : this.getState().getVariable("sys", "thread_id").toString();
    }

    public long elapsedTime(LocalDateTime etime) {
        return Duration.between(ctime, etime).toMillis();
    }
}
