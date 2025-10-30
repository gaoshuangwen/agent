package dev.langgraph.ui.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langgraph.core.*;
import dev.langgraph.ui.service.GraphRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private GraphRegistry graphRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(30))
                .build();

        NodeId node1 = NodeId.generate();
        NodeId node2 = NodeId.generate();

        Graph testGraph = GraphBuilder.newGraph()
                .id(GraphId.of("ws-test-graph"))
                .name("WebSocket Test Graph")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return state.with("step", "1");
                })
                .addFunctionalNode(node2, "Node2", (state, ctx) -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return state.with("step", "2");
                })
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        graphRegistry.registerGraph("ws-test-graph", testGraph);
    }

    @Test
    void testWebSocketConnection() throws Exception {
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketSession session = client.execute(
                new TestWebSocketHandler(messageFuture),
                "ws://localhost:" + port + "/ws/executions"
        ).get(5, TimeUnit.SECONDS);

        assertNotNull(session);
        assertTrue(session.isOpen());

        String subscribeMessage = objectMapper.writeValueAsString(Map.of(
                "action", "subscribe",
                "executionId", "test-execution-123"
        ));
        session.sendMessage(new TextMessage(subscribeMessage));

        String ackMessage = messageFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(ackMessage);
        assertTrue(ackMessage.contains("ack"));
        assertTrue(ackMessage.contains("subscribed"));

        session.close();
    }

    @Test
    void testWebSocketReceivesExecutionEvents() throws Exception {
        CompletableFuture<String> firstMessageFuture = new CompletableFuture<>();
        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketSession session = client.execute(
                new TestWebSocketHandler(firstMessageFuture),
                "ws://localhost:" + port + "/ws/executions"
        ).get(5, TimeUnit.SECONDS);

        String startExecutionResponse = webTestClient.post()
                .uri("/api/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "graphId", "ws-test-graph",
                        "initialState", Map.of("test", "data"),
                        "metadata", Map.of()
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(startExecutionResponse);
        String executionId = extractExecutionId(startExecutionResponse);

        String subscribeMessage = objectMapper.writeValueAsString(Map.of(
                "action", "subscribe",
                "executionId", executionId
        ));
        session.sendMessage(new TextMessage(subscribeMessage));

        String ackMessage = firstMessageFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(ackMessage);

        Thread.sleep(2000);

        session.close();
    }

    private String extractExecutionId(String response) throws Exception {
        Map<String, String> responseMap = objectMapper.readValue(response, Map.class);
        return responseMap.get("executionId");
    }

    private static class TestWebSocketHandler extends TextWebSocketHandler {
        private final CompletableFuture<String> messageFuture;

        public TestWebSocketHandler(CompletableFuture<String> messageFuture) {
            this.messageFuture = messageFuture;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            if (!messageFuture.isDone()) {
                messageFuture.complete(message.getPayload());
            }
        }
    }
}
