package com.ke.bella.workflow.node;

import java.util.stream.Stream;

public enum NodeType {
    START("start"),
    END("end"),
    IF_ELSE("if-else"),
    ITERATION("iteration"),
    LLM("llm"),
    HTTP_REQUEST("http-request"),
    QUESTION_CLASSIFIER("question-classifier"),
    TEMPLATE_TRANSFORM("template-transform"),
    KNOWLEDGE_RETRIEVAL("knowledge-retrieval"),
    PARAMETER_EXTRACTOR("parameter-extractor"),
    CODE("code"),
    TOOL("tool"),
    ;

    public final String name;

    private NodeType(String name) {
        this.name = name;
    }

    public static NodeType of(String name) {
        return Stream.of(NodeType.values()).filter(nodeType -> nodeType.name.equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown node type: " + name));
    }
}
