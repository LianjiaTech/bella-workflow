package com.ke.bella.workflow.api.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public enum ModelFeature {
    TOOL_CALL("tool-call"),
    MULTI_TOOL_CALL("multi-tool-call"),
    AGENT_THOUGHT("agent-thought"),
    VISION("vision"),
    STREAM_TOOL_CALL("stream-tool-call");

    @Getter
    private final String value;

    ModelFeature(String value) {
        this.value = value;
    }

    public static List<String> features(boolean toolCall, boolean multiToolCall,
            boolean agentThought, boolean vision, boolean streamToolCall) {
        List<String> features = new ArrayList<>();
        if(toolCall) {
            features.add(TOOL_CALL.value);
        }
        if(multiToolCall) {
            features.add(MULTI_TOOL_CALL.value);
        }
        if(agentThought) {
            features.add(AGENT_THOUGHT.value);
        }
        if(vision) {
            features.add(VISION.value);
        }
        if(streamToolCall) {
            features.add(STREAM_TOOL_CALL.value);
        }
        return features;
    }
}
