package com.ke.bella.workflow.node;

public enum NodeType {
    START("start");

    public final String name;

    private NodeType(String name) {
        this.name = name;
    }
}
