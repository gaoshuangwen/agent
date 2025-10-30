package dev.langgraph.integrations.langchain4j;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LLMNodeTest {

    @Mock
    private ChatLanguageModel mockModel;

    private GraphContext graphContext;
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        graphContext = GraphContext.of(GraphId.of("test-graph"));
        executionContext = ExecutionContext.create(graphContext);
    }

    @Test
    void shouldExecuteLLMNode() throws Exception {
        when(mockModel.generate(anyString())).thenReturn("Hello, World!");

        LLMNode node = LLMNode.builder(NodeId.of("llm"), "LLM", mockModel)
                .prompt("Say hello to {{name}}")
                .build();

        State inputState = State.empty().with("name", "World");
        State outputState = node.execute(inputState, executionContext, EventPublisher.noOp());

        assertEquals("Hello, World!", outputState.get("llm_response", ""));
        assertNotNull(outputState.get("__llm_prompt__"));
    }

    @Test
    void shouldUseCustomOutputKey() throws Exception {
        when(mockModel.generate(anyString())).thenReturn("Response");

        LLMNode node = LLMNode.builder(NodeId.of("llm"), "LLM", mockModel)
                .prompt("Test")
                .outputKey("custom_output")
                .build();

        State outputState = node.execute(State.empty(), executionContext, EventPublisher.noOp());

        assertEquals("Response", outputState.get("custom_output", ""));
    }

    @Test
    void shouldRenderPromptTemplateWithStateData() throws Exception {
        when(mockModel.generate("Hello Alice, you are 30 years old"))
                .thenReturn("Nice to meet you!");

        LLMNode node = LLMNode.builder(NodeId.of("llm"), "LLM", mockModel)
                .prompt("Hello {{name}}, you are {{age}} years old")
                .build();

        State inputState = State.empty()
                .with("name", "Alice")
                .with("age", 30);

        State outputState = node.execute(inputState, executionContext, EventPublisher.noOp());

        assertEquals("Nice to meet you!", outputState.get("llm_response", ""));
        assertEquals("Hello Alice, you are 30 years old", outputState.get("__llm_prompt__", ""));
    }

    @Test
    void shouldStoreModelInformation() throws Exception {
        when(mockModel.generate(anyString())).thenReturn("Response");

        LLMConfig config = LLMConfig.gpt4();
        LLMNode node = LLMNode.builder(NodeId.of("llm"), "LLM", mockModel)
                .prompt("Test")
                .config(config)
                .build();

        State outputState = node.execute(State.empty(), executionContext, EventPublisher.noOp());

        assertEquals("gpt-4", outputState.get("__llm_model__", ""));
    }

    @Test
    void shouldThrowWhenPromptTemplateNotSet() {
        assertThrows(IllegalStateException.class, () -> {
            LLMNode.builder(NodeId.of("llm"), "LLM", mockModel).build();
        });
    }

    @Test
    void shouldHandleComplexPromptTemplate() throws Exception {
        when(mockModel.generate(anyString())).thenReturn("Analyzed successfully");

        PromptTemplate complexTemplate = PromptTemplate.builder()
                .template("""
                        System: You are a {{role}}.
                        User: {{query}}
                        Context: {{context}}
                        """)
                .defaultVariable("role", "assistant")
                .build();

        LLMNode node = LLMNode.builder(NodeId.of("llm"), "LLM", mockModel)
                .promptTemplate(complexTemplate)
                .build();

        State inputState = State.empty()
                .with("query", "Analyze this")
                .with("context", "Important data");

        State outputState = node.execute(inputState, executionContext, EventPublisher.noOp());

        assertEquals("Analyzed successfully", outputState.get("llm_response", ""));
    }
}
