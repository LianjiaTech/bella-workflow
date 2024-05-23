package com.ke.bella.workflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.graph.Graphs;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.google.common.graph.Traverser;
import com.ke.bella.workflow.WorkflowSchema.Edge;
import com.ke.bella.workflow.WorkflowSchema.Node;
import com.ke.bella.workflow.node.NodeType;

public class WorkflowGraph {
    final WorkflowSchema meta;
    final WorkflowSchema.Node start;
    final Map<String, WorkflowSchema.Node> nodeMap;
    final Map<String, WorkflowSchema.Edge> edgeMap;
    final MutableNetwork<WorkflowSchema.Node, WorkflowSchema.Edge> graph;

    public WorkflowGraph(WorkflowSchema meta) {
        this.meta = meta;
        this.nodeMap = meta.getGraph().getNodes()
                .stream()
                .collect(Collectors.toMap(WorkflowSchema.Node::getId, n -> n));
        this.edgeMap = meta.getGraph().getEdges()
                .stream()
                .collect(Collectors.toMap(WorkflowSchema.Edge::getKey, e -> e));
        this.start = meta.getGraph().getNodes()
                .stream()
                .filter(n -> NodeType.START.name.equals(n.getType()))
                .findFirst()
                .get();
        this.graph = buildGraph(meta, this.nodeMap);
    }

    public Node getStartNode() {
        return this.start;
    }

    public Node node(String id) {
        return nodeMap.get(id);
    }

    public List<WorkflowSchema.Edge> inEdges(String id) {
        return new ArrayList<>(graph.inEdges(node(id)));
    }

    public List<WorkflowSchema.Edge> outEdges(String id) {
        return new ArrayList<>(graph.outEdges(node(id)));
    }

    public Set<String> nodeIds() {
        return new HashSet<>(nodeMap.keySet());
    }

    private static MutableNetwork<Node, Edge> buildGraph(WorkflowSchema meta, Map<String, Node> nodes) {
        MutableNetwork<WorkflowSchema.Node, WorkflowSchema.Edge> graph = NetworkBuilder
                .directed()
                .allowsParallelEdges(true)
                .allowsSelfLoops(false)
                .build();

        nodes.values().forEach(graph::addNode);
        meta.getGraph().getEdges().forEach(e -> graph.addEdge(nodes.get(e.getSource()), nodes.get(e.getTarget()), e));

        return graph;
    }

    public void validate() {
        // 判断有且只有一个start
        int startCount = meta.getGraph().getNodes()
                .stream()
                .filter(n -> NodeType.START.name.equals(n.getType()))
                .collect(Collectors.counting()).intValue();
        if(startCount != 1) {
            throw new IllegalArgumentException("工作流必须有且只有一个start");
        }

        // 判断是否有环
        boolean hasCycle = Graphs.hasCycle(this.graph);
        if(hasCycle) {
            throw new IllegalArgumentException("工作流不能有环");
        }

        // 从start出发可以遍历到所有节点
        Traverser<WorkflowSchema.Node> traverser = Traverser.forGraph(this.graph);
        Iterable<Node> it = traverser.breadthFirst(start);
        Set<Node> reachableNodes = new HashSet<>();
        it.forEach(reachableNodes::add);
        for (Node node : this.graph.nodes()) {
            if(!reachableNodes.contains(node)) {
                throw new IllegalArgumentException("工作流不连通，存在孤立节点： " + node.getId());
            }
        }

        // 所有终止节点都必须是END
        for (Node node : this.graph.nodes()) {
            if(this.graph.outDegree(node) == 0 && !NodeType.END.name.equals(node.getType())) {
                throw new IllegalArgumentException("所有终止节点都必须是END，存在非END节点： " + node.getId());
            }
        }
    }
}
