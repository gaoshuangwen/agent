package dev.langgraph.core.message;

import dev.langgraph.core.Graph;
import dev.langgraph.core.execution.ExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MessageConcurrencyTest {

    @Test
    void shouldHandleConcurrentExecutions() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        
        Graph graph = MessageGraphBuilder.newMessageGraph("Concurrent")
                .addResponder("responder", "Responder", ctx -> {
                    int count = counter.incrementAndGet();
                    return "Response " + count;
                })
                .entryPoint("responder")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        
        int numExecutions = 10;
        List<CompletableFuture<ExecutionResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < numExecutions; i++) {
            futures.add(messageGraph.executeAsync("Message " + i));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        
        assertEquals(numExecutions, counter.get());
        
        for (CompletableFuture<ExecutionResult> future : futures) {
            ExecutionResult result = future.get();
            assertTrue(result.isSuccess());
            assertEquals(2, messageGraph.extractHistory(result).size());
        }
        
        messageGraph.shutdown();
    }

    @Test
    void shouldMaintainStateConsistency() throws Exception {
        Graph graph = MessageGraphBuilder.newMessageGraph("StateConsistency")
                .addResponder("responder", "Responder", ctx -> {
                    int messageCount = ctx.history().size();
                    return "Processed " + messageCount + " messages";
                })
                .entryPoint("responder")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        
        List<CompletableFuture<ExecutionResult>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(messageGraph.executeAsync("Test " + i));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        
        for (CompletableFuture<ExecutionResult> future : futures) {
            ExecutionResult result = future.get();
            assertTrue(result.isSuccess());
            MessageHistory history = messageGraph.extractHistory(result);
            assertEquals(2, history.size());
        }
        
        messageGraph.shutdown();
    }

    @Test
    void shouldHandleParallelMessageProcessing() throws Exception {
        AtomicInteger processCount = new AtomicInteger(0);
        
        Graph graph = MessageGraphBuilder.newMessageGraph("Parallel")
                .addProducer("producer", "Producer", ctx -> {
                    processCount.incrementAndGet();
                    return Message.system("System");
                })
                .addResponder("responder1", "Responder1", ctx -> {
                    processCount.incrementAndGet();
                    return "Response 1";
                })
                .addResponder("responder2", "Responder2", ctx -> {
                    processCount.incrementAndGet();
                    return "Response 2";
                })
                .addEdge("producer", "responder1")
                .addEdge("producer", "responder2")
                .entryPoint("producer")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        ExecutionResult result = messageGraph.execute("Test");

        assertTrue(result.isSuccess());
        assertTrue(processCount.get() >= 3);
        
        messageGraph.shutdown();
    }

    @Test
    void shouldHandleConversationContinuationConcurrently() throws Exception {
        Graph graph = MessageGraphBuilder.newMessageGraph("ConvContinuation")
                .addResponder("responder", "Responder", ctx -> "Response")
                .entryPoint("responder")
                .build();

        MessageGraph messageGraph = MessageGraph.of(graph);
        
        ExecutionResult initial = messageGraph.execute("Initial");
        
        List<CompletableFuture<ExecutionResult>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Message newMsg = Message.user("Follow up " + i);
            futures.add(CompletableFuture.supplyAsync(() -> 
                messageGraph.continueConversation(initial, newMsg)));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        
        for (CompletableFuture<ExecutionResult> future : futures) {
            ExecutionResult result = future.get();
            assertTrue(result.isSuccess());
            MessageHistory history = messageGraph.extractHistory(result);
            assertEquals(4, history.size());
        }
        
        messageGraph.shutdown();
    }
}
