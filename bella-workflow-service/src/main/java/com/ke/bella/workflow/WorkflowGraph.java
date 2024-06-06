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
import com.ke.bella.workflow.node.BaseNode;
import com.ke.bella.workflow.node.NodeType;

import lombok.Getter;

public class WorkflowGraph {
    @Getter
    final WorkflowSchema meta;
    final WorkflowSchema.Node start;
    final Map<String, WorkflowSchema.Node> nodeMap;
    final Map<String, WorkflowSchema.Edge> edgeMap;
    final MutableNetwork<String, WorkflowSchema.Edge> graph;

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
        return new ArrayList<>(graph.inEdges(id));
    }

    public List<WorkflowSchema.Edge> outEdges(String id) {
        return new ArrayList<>(graph.outEdges(id));
    }

    public Set<String> nodeIds() {
        return new HashSet<>(nodeMap.keySet());
    }

    private static MutableNetwork<String, Edge> buildGraph(WorkflowSchema meta, Map<String, Node> nodes) {
        MutableNetwork<String, WorkflowSchema.Edge> graph = NetworkBuilder
                .directed()
                .allowsParallelEdges(true)
                .allowsSelfLoops(false)
                .build();

        nodes.values().forEach(n -> graph.addNode(n.getId()));
        meta.getGraph().getEdges().forEach(e -> graph.addEdge(e.getSource(), e.getTarget(), e));

        return graph;
    }

    public void validate() {
        // 判断是否存在对应的实现类
        meta.getGraph().getNodes().forEach(BaseNode::from);

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
        Traverser<String> traverser = Traverser.forGraph(this.graph);
        Iterable<String> it = traverser.breadthFirst(start.getId());
        Set<String> reachableNodes = new HashSet<>();
        it.forEach(reachableNodes::add);
        for (String nodeId : this.graph.nodes()) {
            if(!reachableNodes.contains(nodeId)) {
                throw new IllegalArgumentException("工作流不连通，存在孤立节点： " + nodeId);
            }

            Integer handleSize = (Integer) node(nodeId).getData().get("source_handles_size");
            if(handleSize != null && handleSize.intValue() != outEdges(nodeId).size()) {
                throw new IllegalArgumentException("工作流不连通，节点的后续边不全： " + nodeId);
            }
        }

        // 所有终止节点都必须是END
        for (String nodeId : this.graph.nodes()) {
            if(this.graph.outDegree(nodeId) == 0 && !NodeType.END.name.equals(node(nodeId).getType())) {
                throw new IllegalArgumentException("所有终止节点都必须是END，存在非END节点： " + nodeId);
            }
        }
    }
}
