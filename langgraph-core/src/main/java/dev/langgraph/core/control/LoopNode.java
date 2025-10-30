package dev.langgraph.core.control;

import dev.langgraph.core.*;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public final class LoopNode extends AbstractNode {
    
    private final GuardCondition condition;
    private final BiFunction<State, ExecutionContext, State> body;
    private final int maxIterations;

    public LoopNode(NodeId id, String name, Map<String, Object> metadata,
                   GuardCondition condition, BiFunction<State, ExecutionContext, State> body,
                   int maxIterations) {
        super(id, name, metadata);
        this.condition = Objects.requireNonNull(condition, "Condition cannot be null");
        this.body = Objects.requireNonNull(body, "Body function cannot be null");
        if (maxIterations < 1) {
            throw new IllegalArgumentException("Max iterations must be at least 1");
        }
        this.maxIterations = maxIterations;
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) {
        State currentState = inputState;
        int iteration = 0;

        while (condition.test(currentState, context)) {
            iteration++;
            
            if (iteration > maxIterations) {
                throw new LoopDetector.InfiniteLoopException(
                        "Loop exceeded maximum iterations (" + maxIterations + ") in node " + id()
                );
            }

            currentState = body.apply(currentState, context);
            currentState = currentState.with("__loop_iteration__", iteration);
        }

        return currentState.with("__loop_completed__", true);
    }

    public static Builder builder(NodeId id, String name) {
        return new Builder(id, name);
    }

    public static final class Builder {
        private final NodeId id;
        private final String name;
        private final Map<String, Object> metadata = new java.util.HashMap<>();
        private GuardCondition condition;
        private BiFunction<State, ExecutionContext, State> body;
        private int maxIterations = 1000;

        private Builder(NodeId id, String name) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
        }

        public Builder condition(GuardCondition condition) {
            this.condition = Objects.requireNonNull(condition);
            return this;
        }

        public Builder body(BiFunction<State, ExecutionContext, State> body) {
            this.body = Objects.requireNonNull(body);
            return this;
        }

        public Builder maxIterations(int maxIterations) {
            if (maxIterations < 1) {
                throw new IllegalArgumentException("Max iterations must be at least 1");
            }
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public LoopNode build() {
            if (condition == null) {
                throw new IllegalStateException("Condition must be set");
            }
            if (body == null) {
                throw new IllegalStateException("Body function must be set");
            }
            return new LoopNode(id, name, metadata, condition, body, maxIterations);
        }
    }
}
