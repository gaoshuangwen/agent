package dev.langgraph.core.control;

import dev.langgraph.core.*;
import dev.langgraph.core.execution.ExecutionConfig;
import dev.langgraph.core.execution.ExecutionResult;
import dev.langgraph.core.execution.StateGraph;

import java.util.Map;
import java.util.Objects;

public final class SubgraphNode extends AbstractNode {
    
    private final Graph subgraph;
    private final ExecutionConfig config;
    private final int maxDepth;
    private StateGraph stateGraph;

    public SubgraphNode(NodeId id, String name, Map<String, Object> metadata,
                       Graph subgraph, ExecutionConfig config, int maxDepth) {
        super(id, name, metadata);
        this.subgraph = Objects.requireNonNull(subgraph, "Subgraph cannot be null");
        this.config = config != null ? config : ExecutionConfig.defaults();
        if (maxDepth < 1) {
            throw new IllegalArgumentException("Max depth must be at least 1");
        }
        this.maxDepth = maxDepth;
        this.stateGraph = StateGraph.of(subgraph, this.config);
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) {
        int currentDepth = inputState.get("__recursion_depth__", 0);
        
        if (currentDepth >= maxDepth) {
            throw new RecursionDepthExceededException(
                    "Maximum recursion depth (" + maxDepth + ") exceeded in subgraph " + id()
            );
        }

        State subgraphInput = inputState.with("__recursion_depth__", currentDepth + 1);
        ExecutionResult result = stateGraph.execute(subgraphInput, context.graphContext());

        if (result.isFailed()) {
            throw new SubgraphExecutionException(
                    "Subgraph execution failed", 
                    result.error().orElse(new RuntimeException("Unknown error"))
            );
        }

        return result.finalState().without("__recursion_depth__");
    }

    public void shutdown() {
        if (stateGraph != null) {
            stateGraph.shutdown();
        }
    }

    public static Builder builder(NodeId id, String name) {
        return new Builder(id, name);
    }

    public static final class Builder {
        private final NodeId id;
        private final String name;
        private final Map<String, Object> metadata = new java.util.HashMap<>();
        private Graph subgraph;
        private ExecutionConfig config;
        private int maxDepth = 10;

        private Builder(NodeId id, String name) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
        }

        public Builder subgraph(Graph subgraph) {
            this.subgraph = Objects.requireNonNull(subgraph);
            return this;
        }

        public Builder config(ExecutionConfig config) {
            this.config = config;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            if (maxDepth < 1) {
                throw new IllegalArgumentException("Max depth must be at least 1");
            }
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public SubgraphNode build() {
            if (subgraph == null) {
                throw new IllegalStateException("Subgraph must be set");
            }
            return new SubgraphNode(id, name, metadata, subgraph, config, maxDepth);
        }
    }

    public static class RecursionDepthExceededException extends RuntimeException {
        public RecursionDepthExceededException(String message) {
            super(message);
        }
    }

    public static class SubgraphExecutionException extends RuntimeException {
        public SubgraphExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
