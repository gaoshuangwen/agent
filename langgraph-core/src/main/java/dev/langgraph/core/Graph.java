package dev.langgraph.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Graph {
    
    GraphId id();
    
    String name();
    
    Map<String, Object> metadata();
    
    List<Node> nodes();
    
    List<Edge> edges();
    
    Optional<Node> getNode(NodeId nodeId);
    
    List<Edge> getOutgoingEdges(NodeId nodeId);
    
    List<Edge> getIncomingEdges(NodeId nodeId);
    
    Optional<NodeId> entryPoint();
    
    void validate() throws GraphValidationException;
}
