package com.ke.bella.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

public class WorkflowRunState {
    /** NodeId -> NodeRunResult */
    final Map<String, NodeRunResult> nodeCompletedStates = new HashMap<>();
    final Map<String, NodeRunResult> nodeWaitingStates = new HashMap<>();
    final Set<String> activatedSourceHandles = new HashSet<>();

    synchronized boolean isEmpty() {
        return nodeCompletedStates.isEmpty();
    }

    synchronized Set<String> completedNodeIds() {
        return nodeCompletedStates.keySet();
    }

    synchronized Set<String> waitingNodeIds() {
        return nodeWaitingStates.keySet();
    }

    synchronized boolean isActivated(String sourceNodeId, String sourceHandle) {
        return activatedSourceHandles.contains(String.format("%s/%s", sourceNodeId, sourceHandle));
    }

    synchronized void putNodeState(String nodeId, NodeRunResult state) {
        NodeRunResult.Status s = state.status;
        if(s == NodeRunResult.Status.running) {
            throw new IllegalStateException("工作流节点运行状态异常");
        } else if(s == NodeRunResult.Status.waiting) {
            nodeWaitingStates.put(nodeId, state);
        } else if(s == NodeRunResult.Status.succeeded) {
            nodeCompletedStates.put(nodeId, state);
            state.activatedSourceHandles
                    .forEach(h -> activatedSourceHandles.add(String.format("%s/%s", nodeId, h)));
        } else {
            nodeCompletedStates.put(nodeId, state);
        }
    }

    @Data
    @SuperBuilder
    @SuppressWarnings("rawtypes")
    public static class NodeRunResult {
        public enum Status {
            running,
            waiting,
            succeeded,
            failed;
        }

        Map inputs;
        Map processData;
        Map outputs;
        Exception error;
        Status status;

        @Builder.Default
        List<String> activatedSourceHandles = new ArrayList();

    }
}
