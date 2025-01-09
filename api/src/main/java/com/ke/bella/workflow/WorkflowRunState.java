package com.ke.bella.workflow;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ke.bella.openapi.BellaContext;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

public class WorkflowRunState implements Serializable {
    private static final long serialVersionUID = 734805247655179406L;

    /** NodeId -> NodeRunResult */
    @JsonProperty
    Map<String, NodeRunResult> nodeCompletedStates = new HashMap<>();
    @JsonProperty
    Map<String, NodeRunResult> nodeWaitingStates = new HashMap<>();
    @SuppressWarnings("rawtypes")
    @JsonProperty
    Map<String, Map> variablePool = new HashMap<>();
    @JsonProperty
    Set<String> activatedSourceHandles = new HashSet<>();
    @JsonProperty
    Set<String> deadSourceHandles = new HashSet<>();
    @SuppressWarnings("rawtypes")
    transient Map<String, Map> notifyData = new HashMap<>();
    transient ArrayBlockingQueue<String> nextNodes = new ArrayBlockingQueue<>(64);
    transient int runningNodeCount = 0;

    @Getter
    @Setter
    NodeRunResult workflowRunResult;

    @Getter
    @Setter
    WorkflowRunStatus status;

    public WorkflowRunState() {
        if(BellaContext.getOperatorIgnoreNull() != null) {
            putVariable("sys", "user_id", String.valueOf(BellaContext.getOperator().getUserId()));
            putVariable("sys", "user_name", BellaContext.getOperator().getUserName());
        }

        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        putVariable("sys", "date", date);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WorkflowRunState(Map variablePoolMap) {
        this.variablePool.putAll(variablePoolMap);
    }

    synchronized boolean isEmpty() {
        return nodeCompletedStates.isEmpty();
    }

    synchronized Set<String> completedNodeIds() {
        return nodeCompletedStates.keySet();
    }

    public synchronized Set<String> waitingNodeIds() {
        return nodeWaitingStates.keySet();
    }

    @SuppressWarnings("rawtypes")
    public synchronized void putNotifyData(Map<String, Map> data) {
        notifyData.clear();
        notifyData.putAll(data);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized void putVariable(String nodeId, String key, Object value) {
        Map variables = this.variablePool.get(nodeId);
        if(variables == null) {
            variables = new HashMap();
        }
        variables.put(key, value);
        variablePool.put(nodeId, variables);
    }

    @SuppressWarnings({ "rawtypes" })
    public synchronized Object getVariable(String nodeId, String key) {
        Map variables = this.variablePool.get(nodeId);
        if(variables == null) {
            variables = new HashMap();
        }
        return variables.get(key);
    }

    synchronized boolean isActivated(String sourceNodeId, String sourceHandle) {
        return activatedSourceHandles.contains(String.format("%s/%s", sourceNodeId, sourceHandle));
    }

    synchronized boolean isDeadEdge(String sourceNodeId, String sourceHandle) {
        return deadSourceHandles.contains(String.format("%s/%s", sourceNodeId, sourceHandle));
    }

    @SuppressWarnings("rawtypes")
    public synchronized Map getVariablePool() {
        return Collections.unmodifiableMap(variablePool);
    }

    public synchronized Object getVariableValue(List<String> selector) {
        return Variables.getValue(variablePool, selector);
    }

    public synchronized void putVariableValue(List<String> selector, Object value) {
        Variables.putValue(variablePool, selector, value);
    }

    public synchronized NodeRunResult getNodeState(String nodeId) {
        NodeRunResult r = nodeWaitingStates.get(nodeId);
        return r == null ? nodeCompletedStates.get(nodeId) : r;
    }

    public synchronized boolean isResume(String nodeId) {
        return notifyData.containsKey(nodeId);
    }

    @SuppressWarnings("rawtypes")
    public synchronized Map getNotifyData(String nodeId) {
        return notifyData.get(nodeId);
    }

    public synchronized void putNodeState(String nodeId, NodeRunResult state) {
        putNodeState(nodeId, state, false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized void putNodeState(String nodeId, NodeRunResult state, boolean isResume) {
        NodeRunResult.Status s = state.status;
        if(s == NodeRunResult.Status.running || s == null) {
            throw new IllegalStateException("工作流节点运行状态异常");
        } else if(s == NodeRunResult.Status.waiting || s == NodeRunResult.Status.notified) {
            nodeWaitingStates.put(nodeId, state);
        } else {
            nodeWaitingStates.remove(nodeId);
            if(s == NodeRunResult.Status.succeeded) {
                Map variables = variablePool.get(nodeId);
                if(variables == null) {
                    variables = new HashMap();
                }
                if(state.inputs != null) {
                    variables.putAll(state.inputs);
                }
                if(state.outputs != null) {
                    variables.putAll(state.outputs);
                }
                variablePool.put(nodeId, variables);
                nodeCompletedStates.put(nodeId, state);
                state.activatedSourceHandles
                        .forEach(h -> activatedSourceHandles.add(String.format("%s/%s", nodeId, h)));
                state.deadSourceHandles
                        .forEach(h -> deadSourceHandles.add(String.format("%s/%s", nodeId, h)));
            } else if(s == NodeRunResult.Status.skipped) {
                nodeCompletedStates.put(nodeId, state);
                state.deadSourceHandles
                        .forEach(h -> deadSourceHandles.add(String.format("%s/%s", nodeId, h)));
            } else {
                nodeCompletedStates.put(nodeId, state);
            }
        }

        if(!isResume) {
            if(s != NodeRunResult.Status.skipped && s != NodeRunResult.Status.notified) {
                this.runningNodeCount -= 1;
            }
        }

        if(s == NodeRunResult.Status.failed) {
            setStatus(WorkflowRunStatus.failed);
        }
    }

    synchronized void putNextNode(String nodeId) {
        try {
            nextNodes.put(nodeId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    synchronized void putNextNodes(Collection<String> nodeIds) {
        nextNodes.addAll(nodeIds);
    }

    synchronized List<String> takeNextNodes() {
        List<String> ns = new ArrayList<>();
        nextNodes.drainTo(ns);
        this.runningNodeCount += ns.size();
        return ns;
    }

    synchronized boolean isFinish() {
        return this.runningNodeCount == 0 && nextNodes.isEmpty();
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class NodeRunResult {
        public enum Status {
            running,
            waiting,
            notified,
            succeeded,
            skipped,
            failed;
        }

        Map inputs;
        Map processData;
        Map outputs;
        Exception error;
        Status status;
        long elapsedTime;

        @Builder.Default
        List<String> activatedSourceHandles = new ArrayList();

        @Builder.Default
        List<String> deadSourceHandles = new ArrayList();

        public static NodeRunResult newSkippedResult(List<String> sourceHandles) {
            return NodeRunResult.builder()
                    .status(Status.skipped)
                    .deadSourceHandles(sourceHandles)
                    .build();
        }
    }

    public enum WorkflowRunStatus {
        running,
        succeeded,
        failed,
        stopped,
        suspended;
    }

}
