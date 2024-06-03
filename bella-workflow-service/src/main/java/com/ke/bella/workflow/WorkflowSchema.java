package com.ke.bella.workflow;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
        @JsonAlias({ "value_selector" })
        private List<String> valueSelector;
        private String variable;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public static class Node {
        static final String FIELD_TYPE = "type";
        static final String FIELD_TITLE = "title";

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
            if(data != null && data.containsKey(FIELD_TYPE)) {
                return (String) data.get(FIELD_TYPE);
            }
            return this.type;
        }

        public String getTitle() {
            return (String) data.get(FIELD_TITLE);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private int x;
        private int y;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Viewport {
        private int x;
        private int y;
        private double zoom;
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
