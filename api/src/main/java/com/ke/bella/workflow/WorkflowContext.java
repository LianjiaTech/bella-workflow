package com.ke.bella.workflow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import com.ke.bella.workflow.WorkflowRunState.NodeRunResult;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;
import com.ke.bella.workflow.WorkflowSchema.Edge;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.node.BaseNode;
import com.ke.bella.workflow.node.End;
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
    private String workflowMode;
    private WorkflowGraph graph;
    private WorkflowRunState state;
    private Map userInputs;
    private LocalDateTime ctime;
    private int flashMode;
    private boolean stateful;
    @Builder.Default
    private long nodeTimeout = 300;
    private WorkflowSys sys;

    private Map<String, String> runNodeMapping;

    public Map userInputs() {
        return this.userInputs;
    }

    public void start() {
        this.state.putNextNode(graph.getStartNode().getId());
        this.state.setStatus(WorkflowRunStatus.running);
    }

    public void validate() {
        Assert.isTrue(graph != null, "工作流不能为null");
        Assert.isTrue(state != null, "工作流运行状态不能为null");
        Assert.isTrue(userInputs != null, "userInput不能为null");
        graph.validate(this);
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

    synchronized BaseNode getNode(String nodeId) {
        if(this.isResume(nodeId)) {
            return BaseNode.from(graph.node(nodeId), runNodeMapping.get(nodeId));
        } else {
            return BaseNode.from(graph.node(nodeId));
        }
    }

    public Node getNodeMeta(String nodeId) {
        return graph.node(nodeId);
    }

    public synchronized List<BaseNode> getNextNodes() {
        List<String> ids = state.takeNextNodes();
        return ids.stream()
                .map(this::getNode)
                .collect(Collectors.toList());
    }

    public synchronized boolean isFinish() {
        return state.isFinish();
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

        if(node instanceof End && result.status != NodeRunResult.Status.skipped) {
            this.putWorkflowRunResult(result);
        }

        Set<String> mayDeadIds = new HashSet<>();
        Set<String> successors = this.graph.successors(node.getNodeId());
        for (String n : successors) {
            if(canNodeActive(n)) {
                state.putNextNode(n);
            } else {
                mayDeadIds.add(n);
            }
        }
        skipDeadNodes(mayDeadIds);
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
        return (String) this.getState().getVariable("sys", "thread_id");
    }

    public void setThreadId(String threadId) {
        this.getState().putVariable("sys", "thread_id", threadId);
    }

    public long elapsedTime(LocalDateTime etime) {
        return Duration.between(ctime, etime).toMillis();
    }

    public boolean isFlashMode() {
        return flashMode > 0;
    }

    public synchronized String newMessageId() {
        String msgId = IDGenerator.newMessageId();
        state.putVariable("sys", "message_id", msgId);
        return msgId;
    }

    public synchronized void putResumeNodeMapping(Map<String, String> nodeIds) {
        runNodeMapping = new HashMap<>(nodeIds);
    }
}
