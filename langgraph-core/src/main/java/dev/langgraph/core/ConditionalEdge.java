package dev.langgraph.core;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

public final class ConditionalEdge extends AbstractEdge {
    
    private final BiPredicate<State, ExecutionContext> condition;

    public ConditionalEdge(EdgeId id, NodeId source, NodeId target, Map<String, Object> metadata,
                          int priority, BiPredicate<State, ExecutionContext> condition) {
        super(id, source, target, metadata, priority);
        this.condition = Objects.requireNonNull(condition, "Condition cannot be null");
    }

    @Override
    public boolean canTraverse(State state, ExecutionContext context) {
        return condition.test(state, context);
    }
}
