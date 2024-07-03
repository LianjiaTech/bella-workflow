package com.ke.bella.workflow.node;

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
    ;

    public final String name;

    private NodeType(String name) {
        this.name = name;
    }
}
