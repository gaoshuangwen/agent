package dev.langgraph.core.control;

import dev.langgraph.core.*;
import dev.langgraph.core.execution.ExecutionResult;
import dev.langgraph.core.execution.StateGraph;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BranchMergeTest {

    @Test
    void shouldMergeBranchesWithCombineStrategy() {
        BranchMergeNode merge = BranchMergeNode.builder(NodeId.of("merge"), "Merge")
                .merger(StateMerger.combine())
                .build();

        List<State> branchStates = List.of(
                State.of(Map.of("a", 1, "b", 2)),
                State.of(Map.of("c", 3, "d", 4))
        );

        State inputState = State.of(Map.of("__branch_states__", branchStates));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        State result = merge.doExecute(inputState, context);

        assertTrue(result.has("a"));
        assertTrue(result.has("b"));
        assertTrue(result.has("c"));
        assertTrue(result.has("d"));
        assertFalse(result.has("__branch_states__"));
    }

    @Test
    void shouldMergeBranchesWithFirstStrategy() {
        BranchMergeNode merge = BranchMergeNode.builder(NodeId.of("merge"), "Merge")
                .merger(StateMerger.first())
                .build();

        List<State> branchStates = List.of(
                State.of(Map.of("result", "first")),
                State.of(Map.of("result", "second"))
        );

        State inputState = State.of(Map.of("__branch_states__", branchStates));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        State result = merge.doExecute(inputState, context);

        assertEquals("first", result.get("result", ""));
    }

    @Test
    void shouldMergeBranchesWithLastStrategy() {
        BranchMergeNode merge = BranchMergeNode.builder(NodeId.of("merge"), "Merge")
                .merger(StateMerger.last())
                .build();

        List<State> branchStates = List.of(
                State.of(Map.of("result", "first")),
                State.of(Map.of("result", "second"))
        );

        State inputState = State.of(Map.of("__branch_states__", branchStates));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        State result = merge.doExecute(inputState, context);

        assertEquals("second", result.get("result", ""));
    }

    @Test
    void shouldMergeBranchesWithPriorityStrategy() {
        BranchMergeNode merge = BranchMergeNode.builder(NodeId.of("merge"), "Merge")
                .merger(StateMerger.priorityMerge())
                .build();

        List<State> branchStates = List.of(
                State.of(Map.of("a", 1, "b", 2)),
                State.of(Map.of("a", 10, "c", 3))
        );

        State inputState = State.of(Map.of("__branch_states__", branchStates));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        State result = merge.doExecute(inputState, context);

        assertEquals(1, result.get("a", 0));
        assertEquals(2, result.get("b", 0));
        assertEquals(3, result.get("c", 0));
    }

    @Test
    void shouldMergeWithCustomStrategy() {
        BranchMergeNode merge = BranchMergeNode.builder(NodeId.of("merge"), "Merge")
                .merger(states -> {
                    int sum = 0;
                    for (State state : states) {
                        sum += state.get("value", 0);
                    }
                    return State.of(Map.of("total", sum));
                })
                .build();

        List<State> branchStates = List.of(
                State.of(Map.of("value", 10)),
                State.of(Map.of("value", 20)),
                State.of(Map.of("value", 30))
        );

        State inputState = State.of(Map.of("__branch_states__", branchStates));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        State result = merge.doExecute(inputState, context);

        assertEquals(60, result.get("total", 0));
    }

    @Test
    void shouldHandleEmptyBranchList() {
        BranchMergeNode merge = BranchMergeNode.builder(NodeId.of("merge"), "Merge")
                .merger(StateMerger.combine())
                .build();

        State inputState = State.of(Map.of("other", "value"));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        
        State result = merge.doExecute(inputState, context);

        assertEquals("value", result.get("other", ""));
    }
}
