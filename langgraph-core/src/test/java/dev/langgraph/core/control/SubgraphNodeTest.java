package dev.langgraph.core.control;

import dev.langgraph.core.*;
import dev.langgraph.core.execution.ExecutionResult;
import dev.langgraph.core.execution.StateGraph;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubgraphNodeTest {

    @Test
    void shouldExecuteSubgraph() {
        Graph subgraph = GraphBuilder.newGraph("Subgraph")
                .addFunctionalNode(NodeId.of("process"), "Process", 
                        (state, ctx) -> state.with("processed", true))
                .entryPoint(NodeId.of("process"))
                .build();

        SubgraphNode subgraphNode = SubgraphNode.builder(NodeId.of("sub"), "Subgraph")
                .subgraph(subgraph)
                .build();

        Graph mainGraph = GraphBuilder.newGraph("Main")
                .addNode(subgraphNode)
                .entryPoint(NodeId.of("sub"))
                .build();

        StateGraph stateGraph = StateGraph.of(mainGraph);
        ExecutionResult result = stateGraph.execute(State.of(Map.of("input", "data")));

        assertTrue(result.isSuccess());
        assertTrue(result.finalState().get("processed", false));
        assertEquals("data", result.finalState().get("input", ""));
        
        stateGraph.shutdown();
        subgraphNode.shutdown();
    }

    @Test
    void shouldPreventExcessiveRecursion() {
        Graph subgraph = GraphBuilder.newGraph("Subgraph")
                .addFunctionalNode(NodeId.of("process"), "Process", 
                        (state, ctx) -> state.with("processed", true))
                .entryPoint(NodeId.of("process"))
                .build();

        SubgraphNode subgraphNode = SubgraphNode.builder(NodeId.of("sub"), "Subgraph")
                .subgraph(subgraph)
                .maxDepth(2)
                .build();

        State stateWithHighDepth = State.of(Map.of("__recursion_depth__", 5));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));

        assertThrows(SubgraphNode.RecursionDepthExceededException.class, () -> {
            subgraphNode.doExecute(stateWithHighDepth, context);
        });
        
        subgraphNode.shutdown();
    }

    @Test
    void shouldPropagateSubgraphFailure() {
        Graph failingSubgraph = GraphBuilder.newGraph("Failing")
                .addFunctionalNode(NodeId.of("fail"), "Fail", (state, ctx) -> {
                    throw new RuntimeException("Subgraph error");
                })
                .entryPoint(NodeId.of("fail"))
                .build();

        SubgraphNode subgraphNode = SubgraphNode.builder(NodeId.of("sub"), "Subgraph")
                .subgraph(failingSubgraph)
                .build();

        Graph mainGraph = GraphBuilder.newGraph("Main")
                .addNode(subgraphNode)
                .entryPoint(NodeId.of("sub"))
                .build();

        StateGraph stateGraph = StateGraph.of(mainGraph);
        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isFailed());
        assertTrue(result.error().isPresent());
        
        stateGraph.shutdown();
        subgraphNode.shutdown();
    }

    @Test
    void shouldRequireSubgraph() {
        assertThrows(IllegalStateException.class, () -> {
            SubgraphNode.builder(NodeId.of("sub"), "Sub").build();
        });
    }

    @Test
    void shouldNestedSubgraphs() {
        Graph innerSubgraph = GraphBuilder.newGraph("Inner")
                .addFunctionalNode(NodeId.of("inner"), "Inner", 
                        (state, ctx) -> state.with("inner", true))
                .entryPoint(NodeId.of("inner"))
                .build();

        SubgraphNode innerNode = SubgraphNode.builder(NodeId.of("innerSub"), "InnerSub")
                .subgraph(innerSubgraph)
                .maxDepth(5)
                .build();

        Graph outerSubgraph = GraphBuilder.newGraph("Outer")
                .addNode(innerNode)
                .entryPoint(NodeId.of("innerSub"))
                .build();

        SubgraphNode outerNode = SubgraphNode.builder(NodeId.of("outerSub"), "OuterSub")
                .subgraph(outerSubgraph)
                .maxDepth(5)
                .build();

        Graph mainGraph = GraphBuilder.newGraph("Main")
                .addNode(outerNode)
                .entryPoint(NodeId.of("outerSub"))
                .build();

        StateGraph stateGraph = StateGraph.of(mainGraph);
        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isSuccess());
        assertTrue(result.finalState().get("inner", false));
        
        stateGraph.shutdown();
        outerNode.shutdown();
        innerNode.shutdown();
    }
}
