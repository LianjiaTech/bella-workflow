package com.ke.bella.workflow;

import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;

@lombok.Data
@NoArgsConstructor
public class WorkflowSchema {
    private Graph graph;

    @lombok.Data
    public static class Graph {
        private List<Edge> edges;
        private List<Node> nodes;
        private Viewport viewport;
    }

    @lombok.Data
    public static class Edge {
        private Edge.Data data;
        private String id;
        private String source;
        private String sourceHandle;
        private String target;
        private String targetHandle;
        private String type;

        public String getKey() {
            return String.format("%s/%s-%s/%s", source, sourceHandle, target, targetHandle);
        }

        @lombok.Data
        public static class Data {
            private String sourceType;
            private String targetType;
        }
    }

    @lombok.Data
    public static class Variable {
        private List<String> value_selector;
        private String variable;
    }

    @lombok.Data
    @SuppressWarnings("rawtypes")
    public static class Node {
        private Map data;
        private boolean dragging;
        private int height;
        private String id;
        private Position position;
        private Position positionAbsolute;
        private boolean selected;
        private String sourcePosition;
        private String targetPosition;
        private String type;
        private int width;

        public String getType() {
            if(data != null && data.containsKey("type")) {
                return (String) data.get("type");
            }
            return this.type;
        }
    }

    @lombok.Data
    public static class Position {
        private int x;
        private int y;
    }

    @lombok.Data
    public static class Viewport {
        private int x;
        private int y;
        private int zoom;
    }

    @lombok.Data
    public static class VariableEntity {
        public enum Type {
            TEXT_INPUT("text-input"),
            SELECT("select"),
            PARAGRAPH("paragraph"),
            NUMBER("number");

            private String value;

            Type(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }

            public static Type valueOfLabel(String value) {
                for (Type e : values()) {
                    if(e.value.equals(value)) {
                        return e;
                    }
                }
                throw new IllegalArgumentException("invalid variable type value " + value);
            }
        }

        private String variable;
        private String label;
        private String description;
        private String type;
        private boolean required;
        private Integer maxLength;
        private List<String> options;
        private String defaultVal;
        private String hint;
    }

}
