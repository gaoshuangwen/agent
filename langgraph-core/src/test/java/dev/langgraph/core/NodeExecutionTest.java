package dev.langgraph.core;

import dev.langgraph.core.event.CompositeEventPublisher;
import dev.langgraph.core.event.EventPublisher;
import dev.langgraph.core.event.GraphEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class NodeExecutionTest {

    @Test
    void shouldExecuteNode() throws Exception {
        Node node = NodeBuilder.newNode()
                .name("TestNode")
                .function((state, ctx) -> state.with("executed", true))
                .build();

        State input = State.empty();
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        EventPublisher publisher = EventPublisher.noOp();

        State output = node.execute(input, context, publisher);

        assertTrue(output.has("executed"));
        assertEquals(true, output.get("executed", null));
    }

    @Test
    void shouldTriggerLifecycleHooks() throws Exception {
        AtomicBoolean beforeCalled = new AtomicBoolean(false);
        AtomicBoolean afterCalled = new AtomicBoolean(false);

        Node node = new AbstractNode(NodeId.generate(), "TestNode", Map.of()) {
            @Override
            protected State doExecute(State inputState, ExecutionContext context) {
                return inputState.with("executed", true);
            }

            @Override
            public void onBeforeExecute(State state, ExecutionContext context) {
                beforeCalled.set(true);
            }

            @Override
            public void onAfterExecute(State inputState, State outputState, ExecutionContext context) {
                afterCalled.set(true);
            }
        };

        State input = State.empty();
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        EventPublisher publisher = EventPublisher.noOp();

        node.execute(input, context, publisher);

        assertTrue(beforeCalled.get());
        assertTrue(afterCalled.get());
    }

    @Test
    void shouldTriggerErrorHook() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);
        RuntimeException testException = new RuntimeException("Test error");

        Node node = new AbstractNode(NodeId.generate(), "TestNode", Map.of()) {
            @Override
            protected State doExecute(State inputState, ExecutionContext context) {
                throw testException;
            }

            @Override
            public void onError(State state, ExecutionContext context, Throwable error) {
                errorCalled.set(true);
                assertEquals(testException, error);
            }
        };

        State input = State.empty();
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        EventPublisher publisher = EventPublisher.noOp();

        assertThrows(RuntimeException.class, () -> node.execute(input, context, publisher));
        assertTrue(errorCalled.get());
    }

    @Test
    void shouldPublishExecutionEvents() throws Exception {
        Node node = NodeBuilder.newNode()
                .name("TestNode")
                .function((state, ctx) -> state.with("executed", true))
                .build();

        List<GraphEvent> events = new ArrayList<>();
        CompositeEventPublisher publisher = new CompositeEventPublisher();
        publisher.addListener(events::add);

        State input = State.empty();
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));

        node.execute(input, context, publisher);

        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof GraphEvent.NodeExecutionStarted);
        assertTrue(events.get(1) instanceof GraphEvent.NodeExecutionCompleted);
    }

    @Test
    void shouldPublishFailureEvent() {
        Node node = NodeBuilder.newNode()
                .name("TestNode")
                .function((state, ctx) -> {
                    throw new RuntimeException("Test error");
                })
                .build();

        List<GraphEvent> events = new ArrayList<>();
        CompositeEventPublisher publisher = new CompositeEventPublisher();
        publisher.addListener(events::add);

        State input = State.empty();
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));

        assertThrows(RuntimeException.class, () -> node.execute(input, context, publisher));

        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof GraphEvent.NodeExecutionStarted);
        assertTrue(events.get(1) instanceof GraphEvent.NodeExecutionFailed);
    }

    @Test
    void shouldRespectCanExecuteCondition() {
        Node node = new AbstractNode(NodeId.generate(), "TestNode", Map.of()) {
            @Override
            protected State doExecute(State inputState, ExecutionContext context) {
                return inputState.with("executed", true);
            }

            @Override
            public boolean canExecute(State state, ExecutionContext context) {
                return state.has("canExecute");
            }
        };

        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));

        assertFalse(node.canExecute(State.empty(), context));
        assertTrue(node.canExecute(State.of(Map.of("canExecute", true)), context));
    }
}
