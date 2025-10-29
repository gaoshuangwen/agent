package dev.langgraph.core;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public final class FunctionalNode extends AbstractNode {
    
    private final BiFunction<State, ExecutionContext, State> function;

    public FunctionalNode(NodeId id, String name, Map<String, Object> metadata,
                          BiFunction<State, ExecutionContext, State> function) {
        super(id, name, metadata);
        this.function = Objects.requireNonNull(function, "Function cannot be null");
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) {
        return function.apply(inputState, context);
    }
}
