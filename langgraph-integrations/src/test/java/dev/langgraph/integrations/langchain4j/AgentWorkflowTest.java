package dev.langgraph.integrations.langchain4j;

import dev.langgraph.core.*;
import dev.langgraph.core.execution.ExecutionResult;
import dev.langgraph.core.execution.StateGraph;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

class AgentWorkflowTest {

    @Mock
    private ChatLanguageModel mockModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldExecuteSimpleLLMChain() {
        when(mockModel.generate(anyString())).thenReturn("The capital of France is Paris.");

        Graph graph = ChainBuilder.newChain("SimpleChain")
                .addLLMNode("llm", mockModel, "What is the capital of {{country}}?")
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(State.empty().with("country", "France"));

        assertTrue(result.isSuccess());
        assertEquals("The capital of France is Paris.", result.finalState().get("llm_response", ""));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldExecuteMultiStepChain() {
        when(mockModel.generate(contains("summarize")))
                .thenReturn("This is a summary of the text.");
        when(mockModel.generate(contains("translate")))
                .thenReturn("Ceci est un résumé du texte.");

        Graph graph = ChainBuilder.newChain("MultiStep")
                .addLLMNode("summarize", mockModel, 
                        PromptTemplate.of("Please summarize: {{text}}"),
                        "summary")
                .addLLMNode("translate", mockModel,
                        PromptTemplate.of("Translate to French: {{summary}}"),
                        "translation")
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(
                State.empty().with("text", "Long text here..."));

        assertTrue(result.isSuccess());
        assertEquals("This is a summary of the text.", result.finalState().get("summary", ""));
        assertEquals("Ceci est un résumé du texte.", result.finalState().get("translation", ""));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldExecuteToolBasedAgent() {
        when(mockModel.generate(contains("calculate")))
                .thenReturn("TOOL: add with arguments {\"a\": 5, \"b\": 3}");
        when(mockModel.generate(contains("8")))
                .thenReturn("The result is 8");

        Graph graph = ChainBuilder.newChain("ToolAgent")
                .addFunctionalNode("parse_request", "ParseRequest",
                        (state, ctx) -> state.with("action", "calculate"))
                .addLLMNode("agent", mockModel,
                        PromptTemplate.of("User wants to {{action}}: {{input}}"),
                        "agent_response")
                .addToolNode("execute_tools", Map.of(
                        "add", args -> (int) args.get("a") + (int) args.get("b"),
                        "multiply", args -> (int) args.get("a") * (int) args.get("b")
                ))
                .addLLMNode("respond", mockModel,
                        PromptTemplate.of("The tool result is {{tool_result}}. Respond to user."),
                        "final_response")
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(
                State.empty()
                        .with("input", "add 5 and 3")
                        .with("tool_request", Map.of("tool", "add", "arguments", Map.of("a", 5, "b", 3))));

        assertTrue(result.isSuccess());
        
        stateGraph.shutdown();
    }

    @Test
    void shouldHandleConditionalRouting() {
        when(mockModel.generate(anyString())).thenReturn("This is a question");

        Graph graph = ChainBuilder.newChain("ConditionalAgent")
                .addFunctionalNode("classify", "Classify",
                        (state, ctx) -> {
                            String input = state.get("input", "").toString();
                            boolean isQuestion = input.contains("?");
                            return state.with("is_question", isQuestion);
                        })
                .addLLMNode("answer_question", mockModel,
                        PromptTemplate.of("Answer this question: {{input}}"),
                        "response")
                .addLLMNode("general_response", mockModel,
                        PromptTemplate.of("Respond to: {{input}}"),
                        "response")
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        
        ExecutionResult result1 = stateGraph.execute(
                State.empty().with("input", "What is AI?"));
        assertTrue(result1.isSuccess());

        ExecutionResult result2 = stateGraph.execute(
                State.empty().with("input", "Hello there"));
        assertTrue(result2.isSuccess());
        
        stateGraph.shutdown();
    }

    @Test
    void shouldHandleMultiToolOrchestration() {
        Graph graph = ChainBuilder.newChain("MultiTool")
                .addToolNode("math_tools", Map.of(
                        "add", args -> (int) args.get("a") + (int) args.get("b"),
                        "subtract", args -> (int) args.get("a") - (int) args.get("b"),
                        "multiply", args -> (int) args.get("a") * (int) args.get("b")
                ))
                .addFunctionalNode("format_result", "Format",
                        (state, ctx) -> state.with("formatted", 
                                "Result: " + state.get("tool_result")))
                .build();

        StateGraph stateGraph = StateGraph.of(graph);

        ExecutionResult addResult = stateGraph.execute(
                State.empty().with("tool_request", 
                        Map.of("tool", "add", "arguments", Map.of("a", 10, "b", 5))));
        assertTrue(addResult.isSuccess());
        assertEquals(15, addResult.finalState().get("tool_result"));

        ExecutionResult multiplyResult = stateGraph.execute(
                State.empty().with("tool_request",
                        Map.of("tool", "multiply", "arguments", Map.of("a", 4, "b", 7))));
        assertTrue(multiplyResult.isSuccess());
        assertEquals(28, multiplyResult.finalState().get("tool_result"));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldBuildAgentChainWithTools() {
        when(mockModel.generate(anyString())).thenReturn("Processing request...");

        Graph graph = ChainBuilder.agentChain("TestAgent")
                .model(mockModel)
                .systemPrompt("You are a helpful assistant with access to tools.")
                .tool("get_time", args -> System.currentTimeMillis())
                .tool("get_weather", args -> "Sunny, 72°F")
                .maxIterations(3)
                .build();

        assertNotNull(graph);
        assertTrue(graph.nodes().size() > 0);

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(State.empty().with("input", "Hello"));
        
        assertNotNull(result);
        
        stateGraph.shutdown();
    }

    @Test
    void shouldHandleComplexPromptTemplating() {
        when(mockModel.generate(anyString())).thenReturn("Comprehensive analysis complete");

        PromptTemplate complexTemplate = PromptTemplate.builder()
                .template("""
                        System: You are a {{role}}.
                        
                        Task: {{task}}
                        
                        Context:
                        - User: {{user}}
                        - Priority: {{priority}}
                        - Data: {{data}}
                        
                        Please provide a detailed analysis.
                        """)
                .defaultVariable("role", "data analyst")
                .defaultVariable("priority", "high")
                .build();

        Graph graph = ChainBuilder.newChain("ComplexPrompt")
                .addLLMNode("analyze", mockModel, complexTemplate, "analysis")
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(
                State.empty()
                        .with("task", "Analyze sales data")
                        .with("user", "John")
                        .with("data", "Q4 sales: $1M"));

        assertTrue(result.isSuccess());
        assertEquals("Comprehensive analysis complete", result.finalState().get("analysis", ""));
        
        stateGraph.shutdown();
    }
}
