package dev.langgraph.ui.service;

import dev.langgraph.core.Edge;
import dev.langgraph.core.Graph;
import dev.langgraph.core.Node;
import dev.langgraph.ui.model.EdgeDTO;
import dev.langgraph.ui.model.GraphTopologyDTO;
import dev.langgraph.ui.model.NodeDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GraphService {

    private final GraphRegistry graphRegistry;

    public GraphService(GraphRegistry graphRegistry) {
        this.graphRegistry = graphRegistry;
    }

    public Optional<GraphTopologyDTO> getGraphTopology(String graphId) {
        return graphRegistry.getGraph(graphId)
                .map(this::toTopologyDTO);
    }

    public List<GraphTopologyDTO> getAllGraphs() {
        return graphRegistry.getAllGraphs().stream()
                .map(this::toTopologyDTO)
                .toList();
    }

    private GraphTopologyDTO toTopologyDTO(Graph graph) {
        List<NodeDTO> nodes = graph.nodes().stream()
                .map(this::toNodeDTO)
                .toList();

        List<EdgeDTO> edges = graph.edges().stream()
                .map(this::toEdgeDTO)
                .toList();

        String entryPoint = graph.entryPoint()
                .map(nodeId -> nodeId.value())
                .orElse(null);

        return new GraphTopologyDTO(
                graph.id().value(),
                graph.name(),
                nodes,
                edges,
                entryPoint,
                graph.metadata()
        );
    }

    private NodeDTO toNodeDTO(Node node) {
        return new NodeDTO(
                node.id().value(),
                node.name(),
                node.metadata()
        );
    }

    private EdgeDTO toEdgeDTO(Edge edge) {
        return new EdgeDTO(
                edge.id().value(),
                edge.source().value(),
                edge.target().value(),
                edge.getClass().getSimpleName(),
                edge.metadata()
        );
    }
}
