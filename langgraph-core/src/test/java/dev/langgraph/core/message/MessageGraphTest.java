package dev.langgraph.core.message;

import dev.langgraph.core.Graph;
import dev.langgraph.core.execution.ExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageGraphTest {

    @Test
    void shouldExecuteSimpleConversation() {
        Graph graph = MessageGraphBuilder.newMessageGraph("Simple")
                .addResponder("responder", "Responder", ctx -> "Hello, " + 
                        ctx.history().getLatestByRole("user").map(Message::content).orElse("stranger"))
                .entryPoint("responder")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        ExecutionResult result = messageGraph.execute("John");

        assertTrue(result.isSuccess());
        MessageHistory history = messageGraph.extractHistory(result);
        assertEquals(2, history.size());
        assertEquals("user", history.getMessages().get(0).role());
        assertEquals("John", history.getMessages().get(0).content());
        assertEquals("assistant", history.getMessages().get(1).role());
        assertTrue(history.getMessages().get(1).content().contains("Hello, John"));
        
        messageGraph.shutdown();
    }

    @Test
    void shouldExecuteWithMultipleMessages() {
        Graph graph = MessageGraphBuilder.newMessageGraph("Multi")
                .addResponder("responder", "Responder", ctx -> 
                        "Received " + ctx.history().size() + " messages")
                .entryPoint("responder")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        List<Message> initialMessages = List.of(
                Message.user("First"),
                Message.user("Second")
        );
        
        ExecutionResult result = messageGraph.execute(initialMessages);

        assertTrue(result.isSuccess());
        MessageHistory history = messageGraph.extractHistory(result);
        assertEquals(3, history.size());
        
        messageGraph.shutdown();
    }

    @Test
    void shouldContinueConversation() {
        Graph graph = MessageGraphBuilder.newMessageGraph("Continue")
                .addResponder("responder", "Responder", ctx -> "Response")
                .entryPoint("responder")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        
        ExecutionResult result1 = messageGraph.execute("First message");
        assertEquals(2, messageGraph.extractHistory(result1).size());
        
        ExecutionResult result2 = messageGraph.continueConversation(result1, Message.user("Second message"));
        MessageHistory history = messageGraph.extractHistory(result2);
        
        assertEquals(4, history.size());
        assertEquals("First message", history.getMessages().get(0).content());
        assertEquals("Second message", history.getMessages().get(2).content());
        
        messageGraph.shutdown();
    }

    @Test
    void shouldExecuteWithRouter() {
        Graph graph = MessageGraphBuilder.newMessageGraph("Router")
                .addRouter("router", "Router", ctx -> {
                    String content = ctx.history().getLatestByRole("user")
                            .map(Message::content).orElse("");
                    return content.contains("help") ? "help" : "general";
                })
                .addResponder("helpResponse", "HelpResponse", ctx -> "Help information")
                .addResponder("generalResponse", "GeneralResponse", ctx -> "General response")
                .addRouteEdge("router", "help", "helpResponse")
                .addRouteEdge("router", "general", "generalResponse")
                .entryPoint("router")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        
        ExecutionResult helpResult = messageGraph.execute("I need help");
        MessageHistory helpHistory = messageGraph.extractHistory(helpResult);
        assertTrue(helpHistory.getLatestByRole("assistant").map(Message::content)
                .orElse("").contains("Help information"));
        
        ExecutionResult generalResult = messageGraph.execute("Hello there");
        MessageHistory generalHistory = messageGraph.extractHistory(generalResult);
        assertTrue(generalHistory.getLatestByRole("assistant").map(Message::content)
                .orElse("").contains("General response"));
        
        messageGraph.shutdown();
    }

    @Test
    void shouldExecuteWithMessageTransformer() {
        Graph graph = MessageGraphBuilder.newMessageGraph("Transform")
                .addTransformer("limiter", "Limiter", MessageTransformer.limitToLast(2))
                .addResponder("responder", "Responder", ctx -> 
                        "Processed " + ctx.history().size() + " messages")
                .addEdge("limiter", "responder")
                .entryPoint("limiter")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        List<Message> messages = List.of(
                Message.user("1"),
                Message.user("2"),
                Message.user("3"),
                Message.user("4")
        );
        
        ExecutionResult result = messageGraph.execute(messages);
        MessageHistory history = messageGraph.extractHistory(result);
        
        assertEquals(3, history.size());
        
        messageGraph.shutdown();
    }

    @Test
    void shouldExecuteWithProducer() {
        Graph graph = MessageGraphBuilder.newMessageGraph("Producer")
                .addProducer("producer", "Producer", ctx -> Message.system("System message"))
                .addResponder("responder", "Responder", ctx -> "Response")
                .addEdge("producer", "responder")
                .entryPoint("producer")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        ExecutionResult result = messageGraph.execute(Message.user("User message"));

        MessageHistory history = messageGraph.extractHistory(result);
        assertEquals(3, history.size());
        assertEquals("system", history.getMessages().get(1).role());
        
        messageGraph.shutdown();
    }

    @Test
    void shouldExecuteAsync() throws Exception {
        Graph graph = MessageGraphBuilder.newMessageGraph("Async")
                .addResponder("responder", "Responder", ctx -> "Async response")
                .entryPoint("responder")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        
        ExecutionResult result = messageGraph.executeAsync("Test").get();

        assertTrue(result.isSuccess());
        assertEquals(2, messageGraph.extractHistory(result).size());
        
        messageGraph.shutdown();
    }

    @Test
    void shouldHandleMultipleResponders() {
        Graph graph = MessageGraphBuilder.newMessageGraph("Chain")
                .addResponder("first", "First", ctx -> "First response")
                .addResponder("second", "Second", ctx -> "Second response")
                .addEdge("first", "second")
                .entryPoint("first")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        ExecutionResult result = messageGraph.execute("Start");

        MessageHistory history = messageGraph.extractHistory(result);
        assertEquals(3, history.size());
        assertEquals("user", history.getMessages().get(0).role());
        assertEquals("assistant", history.getMessages().get(1).role());
        assertEquals("assistant", history.getMessages().get(2).role());
        
        messageGraph.shutdown();
    }
}
