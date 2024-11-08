package com.ke.bella.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSchema {
    private Graph graph;

    @JsonIgnore
    @Builder.Default
    private List<EnvVar> environmentVariables = new ArrayList<>();

    @JsonProperty("environment_variables")
    public void setEnvironmentVariables(List<EnvVar> vars) {
        if(vars != null) {
            this.environmentVariables = vars;
        } else {
            vars = new ArrayList<>();
        }
    }


    public WorkflowSchema iterationSchema(String iterationId) {
        List<Edge> es = graph.edges.stream()
                .filter(e -> e.getData().isInIteration() && e.getData().getIterationId().equals(iterationId))
                .collect(Collectors.toList());
        List<Node> ns = graph.nodes.stream()
                .filter(n -> n.isInIteration() && n.getIterationId().equals(iterationId))
                .collect(Collectors.toList());
        Graph subg = Graph.builder().edges(es).nodes(ns).build();
        return WorkflowSchema.builder()
                .graph(subg)
                .build();
    }

    public static WorkflowSchema fromWorkflowDB(WorkflowDB wf) {
        WorkflowSchema schema = JsonUtils.fromJson(wf.getGraph(), WorkflowSchema.class);
        schema.setEnvironmentVariables(JsonUtils.fromJson(wf.getEnvVars(), new TypeReference<List<EnvVar>>() {
        }));
        return schema;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EnvVar {
        String id;
        @JsonProperty("value_type")
        String type;
        String name;
        Object value;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Graph {
        private List<Edge> edges;
        private List<Node> nodes;

        @EqualsAndHashCode.Exclude
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
        @JsonAlias("zIndex")
        @JsonProperty("zIndex")
        private Integer zIndex;

        public String getKey() {
            return String.format("%s/%s-%s/%s", source, sourceHandle, target, targetHandle);
        }

        @lombok.Data
        public static class Data {
            private String sourceType;
            private String targetType;

            @JsonAlias({ "isInIteration" })
            private boolean inIteration;

            @JsonAlias({ "iteration_id" })
            private String iterationId;
        }
    }

    @lombok.Data
    public static class Variable {
        @JsonAlias({ "value_selector" })
        private List<String> valueSelector;
        private String variable;
        private String value;
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

        @EqualsAndHashCode.Exclude
        private boolean dragging;

        @EqualsAndHashCode.Exclude
        private int height;

        private String id;

        @EqualsAndHashCode.Exclude
        private Position position;

        @EqualsAndHashCode.Exclude
        private Position positionAbsolute;

        @EqualsAndHashCode.Exclude
        private boolean selected;

        @EqualsAndHashCode.Exclude
        private String sourcePosition;

        @EqualsAndHashCode.Exclude
        private String targetPosition;

        private String type;

        @EqualsAndHashCode.Exclude
        private int width;

        private String parentId;

        private String extent;

        public String getNodeType() {
            if(data != null && data.containsKey(FIELD_TYPE)) {
                return (String) data.get(FIELD_TYPE);
            }
            return this.type;
        }

        public String getTitle() {
            return (String) data.get(FIELD_TITLE);
        }

        public boolean isInIteration() {
            Object v = data.get("isInIteration");
            return v != null && v.equals(Boolean.TRUE);
        }

        public boolean isIterationStart() {
            Object v = data.get("isIterationStart");
            return v != null && v.equals(Boolean.TRUE);
        }

        public String getIterationId() {
            return (String) data.get("iteration_id");
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
            JSON("json"),
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
        private String varType;
        private boolean required;
        private Integer maxLength;
        private List<String> options;
        private String defaultVal;
        private String hint;
        private List<VariableEntity> children;
    }

}
