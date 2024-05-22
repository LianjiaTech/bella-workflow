package com.ke.bella.workflow.node;

public enum NodeType {
    START("start"),
    END("end");

    public final String name;

    private NodeType(String name) {
        this.name = name;
    }
}
