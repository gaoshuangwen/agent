package dev.langgraph.core;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public final class GraphBuilder {
    
    private GraphId id;
    private String name;
    private final Map<String, Object> metadata = new HashMap<>();
    private final Map<NodeId, Node> nodes = new HashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private NodeId entryPoint;

    private GraphBuilder() {
    }

    public static GraphBuilder newGraph() {
        return new GraphBuilder();
    }

    public static GraphBuilder newGraph(String name) {
        return new GraphBuilder().name(name);
    }

    public GraphBuilder id(GraphId id) {
        this.id = Objects.requireNonNull(id, "Graph ID cannot be null");
        return this;
    }

    public GraphBuilder name(String name) {
        this.name = Objects.requireNonNull(name, "Graph name cannot be null");
        return this;
    }

    public GraphBuilder metadata(String key, Object value) {
        Objects.requireNonNull(key, "Metadata key cannot be null");
        this.metadata.put(key, value);
        return this;
    }

    public GraphBuilder metadata(Map<String, Object> metadata) {
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        this.metadata.putAll(metadata);
        return this;
    }

    public GraphBuilder addNode(Node node) {
        Objects.requireNonNull(node, "Node cannot be null");
        if (nodes.containsKey(node.id())) {
            throw new IllegalArgumentException("Node with ID " + node.id() + " already exists");
        }
        nodes.put(node.id(), node);
        return this;
    }

    public GraphBuilder addFunctionalNode(NodeId nodeId, String nodeName,
                                         BiFunction<State, ExecutionContext, State> function) {
        return addFunctionalNode(nodeId, nodeName, Map.of(), function);
    }

    public GraphBuilder addFunctionalNode(NodeId nodeId, String nodeName, Map<String, Object> metadata,
                                         BiFunction<State, ExecutionContext, State> function) {
        Node node = new FunctionalNode(nodeId, nodeName, metadata, function);
        return addNode(node);
    }

    public GraphBuilder addEdge(Edge edge) {
        Objects.requireNonNull(edge, "Edge cannot be null");
        edges.add(edge);
        return this;
    }

    public GraphBuilder addDirectEdge(NodeId source, NodeId target) {
        return addDirectEdge(EdgeId.generate(), source, target);
    }

    public GraphBuilder addDirectEdge(EdgeId edgeId, NodeId source, NodeId target) {
        return addEdge(new DirectEdge(edgeId, source, target));
    }

    public GraphBuilder addConditionalEdge(NodeId source, NodeId target,
                                          BiPredicate<State, ExecutionContext> condition) {
        return addConditionalEdge(EdgeId.generate(), source, target, condition, 0);
    }

    public GraphBuilder addConditionalEdge(EdgeId edgeId, NodeId source, NodeId target,
                                          BiPredicate<State, ExecutionContext> condition, int priority) {
        return addEdge(new ConditionalEdge(edgeId, source, target, Map.of(), priority, condition));
    }

    public GraphBuilder entryPoint(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "Entry point cannot be null");
        this.entryPoint = nodeId;
        return this;
    }

    public Graph build() {
        if (id == null) {
            id = GraphId.generate();
        }
        if (name == null) {
            name = "Graph-" + id.value();
        }
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Graph must contain at least one node");
        }
        if (entryPoint == null) {
            throw new IllegalStateException("Entry point must be set");
        }
        if (!nodes.containsKey(entryPoint)) {
            throw new IllegalStateException("Entry point node does not exist");
        }

        return new DefaultGraph(id, name, metadata, nodes, edges, entryPoint);
    }

    public Graph buildAndValidate() throws GraphValidationException {
        Graph graph = build();
        graph.validate();
        return graph;
    }
}
