package dev.langgraph.core;

import java.util.*;
import java.util.stream.Collectors;

public final class DefaultGraph implements Graph {
    
    private final GraphId id;
    private final String name;
    private final Map<String, Object> metadata;
    private final Map<NodeId, Node> nodeMap;
    private final List<Edge> edges;
    private final NodeId entryPoint;

    DefaultGraph(GraphId id, String name, Map<String, Object> metadata,
                 Map<NodeId, Node> nodeMap, List<Edge> edges, NodeId entryPoint) {
        this.id = Objects.requireNonNull(id, "Graph ID cannot be null");
        this.name = Objects.requireNonNull(name, "Graph name cannot be null");
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        this.nodeMap = Collections.unmodifiableMap(new HashMap<>(nodeMap));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
        this.entryPoint = entryPoint;
    }

    @Override
    public GraphId id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }

    @Override
    public List<Node> nodes() {
        return new ArrayList<>(nodeMap.values());
    }

    @Override
    public List<Edge> edges() {
        return edges;
    }

    @Override
    public Optional<Node> getNode(NodeId nodeId) {
        return Optional.ofNullable(nodeMap.get(nodeId));
    }

    @Override
    public List<Edge> getOutgoingEdges(NodeId nodeId) {
        return edges.stream()
                .filter(edge -> edge.source().equals(nodeId))
                .sorted(Comparator.comparingInt(Edge::priority).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Edge> getIncomingEdges(NodeId nodeId) {
        return edges.stream()
                .filter(edge -> edge.target().equals(nodeId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NodeId> entryPoint() {
        return Optional.ofNullable(entryPoint);
    }

    @Override
    public void validate() throws GraphValidationException {
        if (nodeMap.isEmpty()) {
            throw new GraphValidationException("Graph must contain at least one node");
        }

        if (entryPoint == null) {
            throw new GraphValidationException("Graph must have an entry point");
        }

        if (!nodeMap.containsKey(entryPoint)) {
            throw new GraphValidationException("Entry point node does not exist in graph");
        }

        for (Edge edge : edges) {
            if (!nodeMap.containsKey(edge.source())) {
                throw new GraphValidationException("Edge source node does not exist: " + edge.source());
            }
            if (!nodeMap.containsKey(edge.target())) {
                throw new GraphValidationException("Edge target node does not exist: " + edge.target());
            }
        }

        Set<NodeId> reachableNodes = findReachableNodes();
        if (reachableNodes.size() < nodeMap.size()) {
            Set<NodeId> unreachable = new HashSet<>(nodeMap.keySet());
            unreachable.removeAll(reachableNodes);
            throw new GraphValidationException("Unreachable nodes detected: " + unreachable);
        }
    }

    private Set<NodeId> findReachableNodes() {
        Set<NodeId> reachable = new HashSet<>();
        Queue<NodeId> queue = new LinkedList<>();
        
        if (entryPoint != null) {
            queue.offer(entryPoint);
            reachable.add(entryPoint);
        }

        while (!queue.isEmpty()) {
            NodeId current = queue.poll();
            for (Edge edge : getOutgoingEdges(current)) {
                if (reachable.add(edge.target())) {
                    queue.offer(edge.target());
                }
            }
        }

        return reachable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultGraph that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Graph{id=" + id + ", name='" + name + "', nodes=" + nodeMap.size() + ", edges=" + edges.size() + "}";
    }
}
