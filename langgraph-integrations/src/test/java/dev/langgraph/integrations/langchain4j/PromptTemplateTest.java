package dev.langgraph.integrations.langchain4j;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplateTest {

    @Test
    void shouldRenderSimpleTemplate() {
        PromptTemplate template = PromptTemplate.of("Hello {{name}}!");
        String result = template.render(Map.of("name", "World"));
        
        assertEquals("Hello World!", result);
    }

    @Test
    void shouldRenderMultipleVariables() {
        PromptTemplate template = PromptTemplate.of("{{greeting}} {{name}}, you are {{age}} years old");
        String result = template.render(Map.of(
                "greeting", "Hello",
                "name", "John",
                "age", 30
        ));
        
        assertEquals("Hello John, you are 30 years old", result);
    }

    @Test
    void shouldUseDefaultVariables() {
        PromptTemplate template = PromptTemplate.of(
                "Hello {{name}}, welcome to {{location}}!",
                Map.of("location", "Earth")
        );
        
        String result = template.render(Map.of("name", "Alice"));
        
        assertEquals("Hello Alice, welcome to Earth!", result);
    }

    @Test
    void shouldOverrideDefaultVariables() {
        PromptTemplate template = PromptTemplate.of(
                "{{greeting}} {{name}}",
                Map.of("greeting", "Hello")
        );
        
        String result = template.render(Map.of(
                "greeting", "Hi",
                "name", "Bob"
        ));
        
        assertEquals("Hi Bob", result);
    }

    @Test
    void shouldThrowOnMissingVariable() {
        PromptTemplate template = PromptTemplate.of("Hello {{name}}!");
        
        assertThrows(IllegalArgumentException.class, () -> {
            template.render(Map.of());
        });
    }

    @Test
    void shouldRenderWithBuilder() {
        PromptTemplate template = PromptTemplate.builder()
                .template("User {{user}} wants to {{action}}")
                .defaultVariable("action", "login")
                .build();
        
        String result = template.render(Map.of("user", "alice"));
        
        assertEquals("User alice wants to login", result);
    }

    @Test
    void shouldAddDefaultVariablesDynamically() {
        PromptTemplate template = PromptTemplate.of("{{a}} + {{b}}")
                .withDefault("b", 10);
        
        String result = template.render(Map.of("a", 5));
        
        assertEquals("5 + 10", result);
    }

    @Test
    void shouldCreateSystemMessage() {
        PromptTemplate template = PromptTemplate.systemMessage("You are a helpful assistant");
        String result = template.render();
        
        assertEquals("System: You are a helpful assistant", result);
    }

    @Test
    void shouldCreateUserMessage() {
        PromptTemplate template = PromptTemplate.userMessage("Hello!");
        String result = template.render();
        
        assertEquals("User: Hello!", result);
    }

    @Test
    void shouldHandleComplexTemplates() {
        String complexTemplate = """
                System: You are a {{role}}.
                
                User Query: {{query}}
                
                Context:
                {{context}}
                
                Please provide a detailed response.
                """;
        
        PromptTemplate template = PromptTemplate.of(complexTemplate);
        String result = template.render(Map.of(
                "role", "technical assistant",
                "query", "How does Java work?",
                "context", "The user is a beginner programmer."
        ));
        
        assertTrue(result.contains("technical assistant"));
        assertTrue(result.contains("How does Java work?"));
        assertTrue(result.contains("beginner programmer"));
    }
}
