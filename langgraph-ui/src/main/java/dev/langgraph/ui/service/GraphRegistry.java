package dev.langgraph.ui.service;

import dev.langgraph.core.Graph;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GraphRegistry {

    private final Map<String, Graph> graphs = new ConcurrentHashMap<>();

    public void registerGraph(String graphId, Graph graph) {
        graphs.put(graphId, graph);
    }

    public Optional<Graph> getGraph(String graphId) {
        return Optional.ofNullable(graphs.get(graphId));
    }

    public List<Graph> getAllGraphs() {
        return new ArrayList<>(graphs.values());
    }

    public void unregisterGraph(String graphId) {
        graphs.remove(graphId);
    }

    public Set<String> getGraphIds() {
        return new HashSet<>(graphs.keySet());
    }
}
