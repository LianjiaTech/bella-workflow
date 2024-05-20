package com.ke.bella.workflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

public class WorkflowRunState {
    /** NodeId -> NodeRunResult */
    final Map<String, NodeRunResult> nodeStates = new HashMap<>();
    final Set<String> activatedSourceHandles = new HashSet<>();

    synchronized boolean isEmpty() {
        return nodeStates.isEmpty();
    }

    synchronized Set<String> nodeIds() {
        return nodeStates.keySet();
    }

    synchronized boolean isActivated(String sourceNodeId, String sourceHandle) {
        return activatedSourceHandles.contains(String.format("%s/%s", sourceNodeId, sourceHandle));
    }

    synchronized void putNodeState(String nodeId, NodeRunResult state) {
        nodeStates.put(nodeId, state);
        state.activatedSourceHandles
                .forEach(h -> activatedSourceHandles.add(String.format("%s/%s", nodeId, h)));
    }

    @Data
    @SuperBuilder
    @SuppressWarnings("rawtypes")
    public static class NodeRunResult {
        Map inputs;
        Map processData;
        Map outputs;
        String error;
        String status;

        @Builder.Default
        List<String> activatedSourceHandles = new ArrayList();

    }
}
