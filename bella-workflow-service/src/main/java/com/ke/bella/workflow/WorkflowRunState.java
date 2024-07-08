package com.ke.bella.workflow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

public class WorkflowRunState {
    /** NodeId -> NodeRunResult */
    final Map<String, NodeRunResult> nodeCompletedStates = new HashMap<>();
    final Map<String, NodeRunResult> nodeWaitingStates = new HashMap<>();
    @SuppressWarnings("rawtypes")
    final Map<String, Map> variablePoolMap = new HashMap<>();
    @SuppressWarnings("rawtypes")
    final Map<String, Map> notifyDataMap = new HashMap<>();
    final Set<String> activatedSourceHandles = new HashSet<>();

    @Getter
    @Setter
    NodeRunResult workflowRunResult;

    @Getter
    @Setter
    WorkflowRunStatus status;

    public WorkflowRunState() {
        if(BellaContext.getOperator() != null) {
            putVariable("sys", "user_id", BellaContext.getOperator().getUserId());
            putVariable("sys", "user_name", BellaContext.getOperator().getUserName());
        }

        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        putVariable("sys", "date", date);
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
        notifyDataMap.clear();
        notifyDataMap.putAll(data);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized void putVariable(String nodeId, String key, Object value) {
        Map variables = this.variablePoolMap.get(nodeId);
        if(variables == null) {
            variables = new HashMap();
        }
        variables.put(key, value);
        variablePoolMap.put(nodeId, variables);
    }

    synchronized boolean isActivated(String sourceNodeId, String sourceHandle) {
        return activatedSourceHandles.contains(String.format("%s/%s", sourceNodeId, sourceHandle));
    }

    @SuppressWarnings("rawtypes")
    public synchronized Map getVariablePool() {
        return Collections.unmodifiableMap(variablePoolMap);
    }

    public synchronized Object getVariableValue(List<String> selector) {
        return Variables.getValue(variablePoolMap, selector);
    }

    public synchronized void putVariableValue(List<String> selector, Object value) {
        Variables.putValue(variablePoolMap, selector, value);
    }

    public synchronized NodeRunResult getNodeState(String nodeId) {
        NodeRunResult r = nodeWaitingStates.get(nodeId);
        return r == null ? nodeCompletedStates.get(nodeId) : r;
    }

    public synchronized boolean isResume(String nodeId) {
        return notifyDataMap.containsKey(nodeId);
    }

    @SuppressWarnings("rawtypes")
    public synchronized Map getNotifyData(String nodeId) {
        return notifyDataMap.get(nodeId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized void putNodeState(String nodeId, NodeRunResult state) {
        NodeRunResult.Status s = state.status;
        if(s == NodeRunResult.Status.running || s == null) {
            throw new IllegalStateException("工作流节点运行状态异常");
        } else if(s == NodeRunResult.Status.waiting || s == NodeRunResult.Status.notified) {
            nodeWaitingStates.put(nodeId, state);
        } else {
            nodeWaitingStates.remove(nodeId);
            if(s == NodeRunResult.Status.succeeded) {
                Map variables = new HashMap();
                if(state.inputs != null) {
                    variables.putAll(state.inputs);
                }
                if(state.outputs != null) {
                    variables.putAll(state.outputs);
                }
                variablePoolMap.put(nodeId, variables);
                nodeCompletedStates.put(nodeId, state);
                state.activatedSourceHandles
                        .forEach(h -> activatedSourceHandles.add(String.format("%s/%s", nodeId, h)));
            } else {
                nodeCompletedStates.put(nodeId, state);
            }
        }
    }

    @Data
    @SuperBuilder
    @SuppressWarnings("rawtypes")
    public static class NodeRunResult {
        public enum Status {
            running,
            waiting,
            notified,
            succeeded,
            failed;
        }

        @Builder.Default
        String nodeRunId = UUID.randomUUID().toString();
        Map inputs;
        Map processData;
        Map outputs;
        Exception error;
        Status status;
        long elapsedTime;

        @Builder.Default
        List<String> activatedSourceHandles = new ArrayList();
    }

    public enum WorkflowRunStatus {
        running,
        succeeded,
        failed,
        stopped,
        suspended;
    }

}
