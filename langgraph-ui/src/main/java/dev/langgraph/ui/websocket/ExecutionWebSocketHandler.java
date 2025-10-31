package dev.langgraph.ui.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langgraph.ui.service.WebSocketNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class ExecutionWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionWebSocketHandler.class);

    private final WebSocketNotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ExecutionWebSocketHandler(WebSocketNotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("Received message from {}: {}", session.getId(), payload);

        Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
        String action = (String) messageData.get("action");

        if ("subscribe".equals(action)) {
            String executionId = (String) messageData.get("executionId");
            if (executionId != null) {
                notificationService.subscribeToExecution(executionId, session);
                sendAck(session, "subscribed", executionId);
            }
        } else if ("unsubscribe".equals(action)) {
            String executionId = (String) messageData.get("executionId");
            if (executionId != null) {
                notificationService.unsubscribeFromExecution(executionId, session);
                sendAck(session, "unsubscribed", executionId);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("WebSocket connection closed: {} with status {}", session.getId(), status);
        notificationService.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket transport error for session {}", session.getId(), exception);
    }

    private void sendAck(WebSocketSession session, String action, String executionId) {
        try {
            Map<String, String> ack = Map.of(
                    "type", "ack",
                    "action", action,
                    "executionId", executionId
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
        } catch (Exception e) {
            logger.error("Failed to send acknowledgment", e);
        }
    }
}
