package dev.langgraph.core.control;

import dev.langgraph.core.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GuardConditionTest {

    @Test
    void shouldTestAlwaysCondition() {
        GuardCondition guard = GuardCondition.always();
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertTrue(guard.test(State.empty(), context));
        assertTrue(guard.test(State.of(Map.of("key", "value")), context));
    }

    @Test
    void shouldTestNeverCondition() {
        GuardCondition guard = GuardCondition.never();
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertFalse(guard.test(State.empty(), context));
        assertFalse(guard.test(State.of(Map.of("key", "value")), context));
    }

    @Test
    void shouldTestHasKeyCondition() {
        GuardCondition guard = GuardCondition.hasKey("myKey");
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertFalse(guard.test(State.empty(), context));
        assertTrue(guard.test(State.of(Map.of("myKey", "value")), context));
    }

    @Test
    void shouldTestEqualsCondition() {
        GuardCondition guard = GuardCondition.equals("status", "ready");
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertFalse(guard.test(State.of(Map.of("status", "pending")), context));
        assertTrue(guard.test(State.of(Map.of("status", "ready")), context));
    }

    @Test
    void shouldTestGreaterThanCondition() {
        GuardCondition guard = GuardCondition.greaterThan("count", 5);
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertFalse(guard.test(State.of(Map.of("count", 3)), context));
        assertFalse(guard.test(State.of(Map.of("count", 5)), context));
        assertTrue(guard.test(State.of(Map.of("count", 7)), context));
    }

    @Test
    void shouldTestLessThanCondition() {
        GuardCondition guard = GuardCondition.lessThan("count", 10);
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertTrue(guard.test(State.of(Map.of("count", 5)), context));
        assertFalse(guard.test(State.of(Map.of("count", 10)), context));
        assertFalse(guard.test(State.of(Map.of("count", 15)), context));
    }

    @Test
    void shouldCombineWithAnd() {
        GuardCondition guard = GuardCondition.hasKey("count")
                .and(GuardCondition.greaterThan("count", 0));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertFalse(guard.test(State.empty(), context));
        assertFalse(guard.test(State.of(Map.of("count", 0)), context));
        assertTrue(guard.test(State.of(Map.of("count", 5)), context));
    }

    @Test
    void shouldCombineWithOr() {
        GuardCondition guard = GuardCondition.equals("status", "ready")
                .or(GuardCondition.equals("status", "active"));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertFalse(guard.test(State.of(Map.of("status", "pending")), context));
        assertTrue(guard.test(State.of(Map.of("status", "ready")), context));
        assertTrue(guard.test(State.of(Map.of("status", "active")), context));
    }

    @Test
    void shouldNegateCondition() {
        GuardCondition guard = GuardCondition.hasKey("error").negate();
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertTrue(guard.test(State.empty(), context));
        assertFalse(guard.test(State.of(Map.of("error", "something")), context));
    }

    @Test
    void shouldCreateCustomCondition() {
        GuardCondition guard = GuardCondition.of((state, ctx) -> 
            state.get("value", 0) % 2 == 0
        );
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        assertFalse(guard.test(State.of(Map.of("value", 3)), context));
        assertTrue(guard.test(State.of(Map.of("value", 4)), context));
    }
}
