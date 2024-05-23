package com.ke.bella.workflow.node;

public enum NodeType {
    START("start"),
    END("end"),
    IF_ELSE("if-else"),
    LLM("llm"),
    HTTP_REQUEST("http-request"),
    QUESTION_CLASSIFIER("question-classifier"),
    ;

    public final String name;

    private NodeType(String name) {
        this.name = name;
    }
}
