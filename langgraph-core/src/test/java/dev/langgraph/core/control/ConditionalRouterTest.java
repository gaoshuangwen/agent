package dev.langgraph.core.control;

import dev.langgraph.core.*;
import dev.langgraph.core.execution.ExecutionResult;
import dev.langgraph.core.execution.StateGraph;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalRouterTest {

    @Test
    void shouldRouteBasedOnCondition() {
        NodeId routerId = NodeId.of("router");
        
        ConditionalRouterNode router = ConditionalRouterNode.builder(routerId, "Router")
                .route("highPriority", GuardCondition.greaterThan("priority", 5))
                .route("lowPriority", GuardCondition.lessThan("priority", 3))
                .defaultRoute("normalPriority")
                .build();

        Graph graph = GraphBuilder.newGraph("ConditionalRoute")
                .addNode(router)
                .addFunctionalNode(NodeId.of("high"), "High", (state, ctx) -> state.with("result", "high"))
                .addFunctionalNode(NodeId.of("normal"), "Normal", (state, ctx) -> state.with("result", "normal"))
                .addFunctionalNode(NodeId.of("low"), "Low", (state, ctx) -> state.with("result", "low"))
                .addConditionalEdge(routerId, NodeId.of("high"), 
                        (state, ctx) -> router.getSelectedRoute(state).equals("highPriority"))
                .addConditionalEdge(routerId, NodeId.of("normal"), 
                        (state, ctx) -> router.getSelectedRoute(state).equals("normalPriority"))
                .addConditionalEdge(routerId, NodeId.of("low"), 
                        (state, ctx) -> router.getSelectedRoute(state).equals("lowPriority"))
                .entryPoint(routerId)
                .build();

        StateGraph stateGraph = StateGraph.of(graph);

        ExecutionResult highResult = stateGraph.execute(State.of(Map.of("priority", 10)));
        assertEquals("high", highResult.finalState().get("result", ""));

        ExecutionResult normalResult = stateGraph.execute(State.of(Map.of("priority", 4)));
        assertEquals("normal", normalResult.finalState().get("result", ""));

        ExecutionResult lowResult = stateGraph.execute(State.of(Map.of("priority", 1)));
        assertEquals("low", lowResult.finalState().get("result", ""));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldUseDefaultRoute() {
        NodeId routerId = NodeId.of("router");
        
        ConditionalRouterNode router = ConditionalRouterNode.builder(routerId, "Router")
                .route("special", GuardCondition.equals("type", "special"))
                .defaultRoute("default")
                .build();

        Graph graph = GraphBuilder.newGraph("DefaultRoute")
                .addNode(router)
                .addFunctionalNode(NodeId.of("special"), "Special", (state, ctx) -> state.with("result", "special"))
                .addFunctionalNode(NodeId.of("default"), "Default", (state, ctx) -> state.with("result", "default"))
                .addConditionalEdge(routerId, NodeId.of("special"), 
                        (state, ctx) -> router.getSelectedRoute(state).equals("special"))
                .addConditionalEdge(routerId, NodeId.of("default"), 
                        (state, ctx) -> router.getSelectedRoute(state).equals("default"))
                .entryPoint(routerId)
                .build();

        StateGraph stateGraph = StateGraph.of(graph);

        ExecutionResult defaultResult = stateGraph.execute(State.of(Map.of("type", "normal")));
        assertEquals("default", defaultResult.finalState().get("result", ""));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldThrowWhenNoRouteMatches() {
        ConditionalRouterNode router = ConditionalRouterNode.builder(NodeId.of("router"), "Router")
                .route("special", GuardCondition.equals("type", "special"))
                .build();

        assertThrows(IllegalStateException.class, () -> {
            router.doExecute(State.of(Map.of("type", "normal")), 
                    ExecutionContext.create(GraphContext.of(GraphId.generate())));
        });
    }

    @Test
    void shouldRequireAtLeastOneRoute() {
        assertThrows(IllegalStateException.class, () -> {
            ConditionalRouterNode.builder(NodeId.of("router"), "Router").build();
        });
    }
}
