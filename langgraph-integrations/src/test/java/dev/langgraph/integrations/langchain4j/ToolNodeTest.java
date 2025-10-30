package dev.langgraph.integrations.langchain4j;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolNodeTest {

    private GraphContext graphContext;
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        graphContext = GraphContext.of(GraphId.of("test-graph"));
        executionContext = ExecutionContext.create(graphContext);
    }

    @Test
    void shouldExecuteTool() throws Exception {
        ToolNode node = ToolNode.builder(NodeId.of("tools"), "Tools")
                .tool("add", args -> {
                    int a = (int) args.get("a");
                    int b = (int) args.get("b");
                    return a + b;
                })
                .build();

        State inputState = State.empty()
                .with("tool_request", Map.of(
                        "tool", "add",
                        "arguments", Map.of("a", 5, "b", 3)
                ));

        State outputState = node.execute(inputState, executionContext, EventPublisher.noOp());

        assertEquals(8, outputState.get("tool_result"));
        assertEquals("add", outputState.get("__tool_name__"));
    }

    @Test
    void shouldExecuteMultipleTools() throws Exception {
        ToolNode node = ToolNode.builder(NodeId.of("tools"), "Tools")
                .tool("multiply", args -> {
                    int a = (int) args.get("a");
                    int b = (int) args.get("b");
                    return a * b;
                })
                .tool("concat", args -> {
                    String a = (String) args.get("a");
                    String b = (String) args.get("b");
                    return a + b;
                })
                .build();

        State inputState1 = State.empty()
                .with("tool_request", Map.of(
                        "tool", "multiply",
                        "arguments", Map.of("a", 4, "b", 5)
                ));

        State outputState1 = node.execute(inputState1, executionContext, EventPublisher.noOp());
        assertEquals(20, outputState1.get("tool_result"));

        State inputState2 = State.empty()
                .with("tool_request", Map.of(
                        "tool", "concat",
                        "arguments", Map.of("a", "Hello ", "b", "World")
                ));

        State outputState2 = node.execute(inputState2, executionContext, EventPublisher.noOp());
        assertEquals("Hello World", outputState2.get("tool_result"));
    }

    @Test
    void shouldHandleToolWithNoArguments() throws Exception {
        ToolNode node = ToolNode.builder(NodeId.of("tools"), "Tools")
                .tool("current_time", args -> System.currentTimeMillis())
                .build();

        State inputState = State.empty()
                .with("tool_request", Map.of("tool", "current_time"));

        State outputState = node.execute(inputState, executionContext, EventPublisher.noOp());

        assertNotNull(outputState.get("tool_result"));
        assertTrue(outputState.get("tool_result") instanceof Long);
    }

    @Test
    void shouldThrowOnUnknownTool() {
        ToolNode node = ToolNode.builder(NodeId.of("tools"), "Tools")
                .tool("add", args -> 0)
                .build();

        State inputState = State.empty()
                .with("tool_request", Map.of("tool", "unknown"));

        assertThrows(IllegalArgumentException.class, () -> {
            node.execute(inputState, executionContext, EventPublisher.noOp());
        });
    }

    @Test
    void shouldThrowOnMissingToolRequest() {
        ToolNode node = ToolNode.builder(NodeId.of("tools"), "Tools")
                .tool("add", args -> 0)
                .build();

        State inputState = State.empty();

        assertThrows(IllegalStateException.class, () -> {
            node.execute(inputState, executionContext, EventPublisher.noOp());
        });
    }

    @Test
    void shouldUseCustomInputOutputKeys() throws Exception {
        ToolNode node = ToolNode.builder(NodeId.of("tools"), "Tools")
                .tool("greet", args -> "Hello " + args.get("name"))
                .inputKey("custom_input")
                .outputKey("custom_output")
                .build();

        State inputState = State.empty()
                .with("custom_input", Map.of(
                        "tool", "greet",
                        "arguments", Map.of("name", "Alice")
                ));

        State outputState = node.execute(inputState, executionContext, EventPublisher.noOp());

        assertEquals("Hello Alice", outputState.get("custom_output"));
    }

    @Test
    void shouldThrowWhenNoToolsRegistered() {
        assertThrows(IllegalStateException.class, () -> {
            ToolNode.builder(NodeId.of("tools"), "Tools").build();
        });
    }

    @Test
    void shouldStoreToolMetadata() throws Exception {
        ToolNode node = ToolNode.builder(NodeId.of("tools"), "Tools")
                .tool("test", args -> "result")
                .build();

        State inputState = State.empty()
                .with("tool_request", Map.of("tool", "test", "arguments", Map.of("x", 1)));

        State outputState = node.execute(inputState, executionContext, EventPublisher.noOp());

        assertEquals("test", outputState.get("__tool_name__"));
        assertEquals(Map.of("x", 1), outputState.get("__tool_arguments__"));
    }
}
