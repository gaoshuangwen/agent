package dev.langgraph.core.control;

import dev.langgraph.core.ExecutionContext;
import dev.langgraph.core.State;

import java.util.Objects;
import java.util.function.BiPredicate;

@FunctionalInterface
public interface GuardCondition {
    
    boolean test(State state, ExecutionContext context);
    
    static GuardCondition of(BiPredicate<State, ExecutionContext> predicate) {
        return predicate::test;
    }
    
    static GuardCondition always() {
        return (state, context) -> true;
    }
    
    static GuardCondition never() {
        return (state, context) -> false;
    }
    
    static GuardCondition hasKey(String key) {
        return (state, context) -> state.has(key);
    }
    
    static GuardCondition equals(String key, Object value) {
        return (state, context) -> Objects.equals(state.get(key, null), value);
    }
    
    static GuardCondition greaterThan(String key, int value) {
        return (state, context) -> {
            Integer stateValue = state.get(key, Integer.MIN_VALUE);
            return stateValue > value;
        };
    }
    
    static GuardCondition lessThan(String key, int value) {
        return (state, context) -> {
            Integer stateValue = state.get(key, Integer.MAX_VALUE);
            return stateValue < value;
        };
    }
    
    default GuardCondition and(GuardCondition other) {
        return (state, context) -> test(state, context) && other.test(state, context);
    }
    
    default GuardCondition or(GuardCondition other) {
        return (state, context) -> test(state, context) || other.test(state, context);
    }
    
    default GuardCondition negate() {
        return (state, context) -> !test(state, context);
    }
}
