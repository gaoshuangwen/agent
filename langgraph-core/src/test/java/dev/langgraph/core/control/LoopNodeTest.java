package dev.langgraph.core.control;

import dev.langgraph.core.*;
import dev.langgraph.core.execution.ExecutionResult;
import dev.langgraph.core.execution.StateGraph;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoopNodeTest {

    @Test
    void shouldExecuteLoopUntilConditionFalse() {
        LoopNode loop = LoopNode.builder(NodeId.of("loop"), "Counter")
                .condition(GuardCondition.lessThan("count", 5))
                .body((state, ctx) -> {
                    int count = state.get("count", 0);
                    return state.with("count", count + 1);
                })
                .maxIterations(10)
                .build();

        Graph graph = GraphBuilder.newGraph("Loop")
                .addNode(loop)
                .entryPoint(NodeId.of("loop"))
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(State.of(Map.of("count", 0)));

        assertTrue(result.isSuccess());
        assertEquals(5, result.finalState().get("count", 0));
        assertEquals(5, result.finalState().get("__loop_iteration__", 0));
        assertTrue(result.finalState().get("__loop_completed__", false));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldPreventInfiniteLoop() {
        LoopNode loop = LoopNode.builder(NodeId.of("loop"), "Infinite")
                .condition(GuardCondition.always())
                .body((state, ctx) -> state.with("tick", true))
                .maxIterations(5)
                .build();

        Graph graph = GraphBuilder.newGraph("InfiniteLoop")
                .addNode(loop)
                .entryPoint(NodeId.of("loop"))
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isFailed());
        assertTrue(result.error().orElseThrow() instanceof LoopDetector.InfiniteLoopException);
        assertTrue(result.error().orElseThrow().getMessage().contains("exceeded maximum iterations"));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldExecuteZeroTimesIfConditionFalse() {
        LoopNode loop = LoopNode.builder(NodeId.of("loop"), "Zero")
                .condition(GuardCondition.never())
                .body((state, ctx) -> state.with("executed", true))
                .build();

        Graph graph = GraphBuilder.newGraph("ZeroLoop")
                .addNode(loop)
                .entryPoint(NodeId.of("loop"))
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isSuccess());
        assertFalse(result.finalState().has("executed"));
        assertTrue(result.finalState().get("__loop_completed__", false));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldRequireConditionAndBody() {
        assertThrows(IllegalStateException.class, () -> {
            LoopNode.builder(NodeId.of("loop"), "Loop").build();
        });

        assertThrows(IllegalStateException.class, () -> {
            LoopNode.builder(NodeId.of("loop"), "Loop")
                    .condition(GuardCondition.always())
                    .build();
        });
    }

    @Test
    void shouldAccumulateStateInLoop() {
        LoopNode loop = LoopNode.builder(NodeId.of("loop"), "Accumulator")
                .condition(GuardCondition.lessThan("sum", 100))
                .body((state, ctx) -> {
                    int sum = state.get("sum", 0);
                    int increment = state.get("increment", 10);
                    return state.with("sum", sum + increment);
                })
                .maxIterations(20)
                .build();

        Graph graph = GraphBuilder.newGraph("Accumulator")
                .addNode(loop)
                .entryPoint(NodeId.of("loop"))
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(State.of(Map.of("sum", 0, "increment", 10)));

        assertTrue(result.isSuccess());
        assertEquals(100, result.finalState().get("sum", 0));
        
        stateGraph.shutdown();
    }
}
