package dev.langgraph.core.control;

import dev.langgraph.core.NodeId;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LoopDetector {
    
    private final int maxIterations;
    private final Map<NodeId, Integer> visitCounts;

    public LoopDetector(int maxIterations) {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("Max iterations must be at least 1");
        }
        this.maxIterations = maxIterations;
        this.visitCounts = new HashMap<>();
    }

    public void recordVisit(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "NodeId cannot be null");
        int count = visitCounts.getOrDefault(nodeId, 0) + 1;
        visitCounts.put(nodeId, count);
        
        if (count > maxIterations) {
            throw new InfiniteLoopException(
                    "Loop limit exceeded for node " + nodeId + 
                    ". Visited " + count + " times, max allowed: " + maxIterations
            );
        }
    }

    public int getVisitCount(NodeId nodeId) {
        return visitCounts.getOrDefault(nodeId, 0);
    }

    public void reset() {
        visitCounts.clear();
    }

    public int maxIterations() {
        return maxIterations;
    }

    public static class InfiniteLoopException extends RuntimeException {
        public InfiniteLoopException(String message) {
            super(message);
        }
    }
}
