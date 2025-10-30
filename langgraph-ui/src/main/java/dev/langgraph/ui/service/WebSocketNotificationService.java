package dev.langgraph.ui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langgraph.core.event.GraphEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class WebSocketNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> executionSubscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebSocketNotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void subscribeToExecution(String executionId, WebSocketSession session) {
        executionSubscriptions.computeIfAbsent(executionId, k -> new CopyOnWriteArraySet<>()).add(session);
        logger.info("Session {} subscribed to execution {}", session.getId(), executionId);
    }

    public void unsubscribeFromExecution(String executionId, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> sessions = executionSubscriptions.get(executionId);
        if (sessions != null) {
            sessions.remove(session);
            logger.info("Session {} unsubscribed from execution {}", session.getId(), executionId);
        }
    }

    public void removeSession(WebSocketSession session) {
        executionSubscriptions.values().forEach(sessions -> sessions.remove(session));
        logger.info("Removed session {} from all subscriptions", session.getId());
    }

    public void publishEvent(GraphEvent event) {
        String executionId = event.executionId();
        CopyOnWriteArraySet<WebSocketSession> sessions = executionSubscriptions.get(executionId);
        if (sessions != null) {
            String message = serializeEvent(event);
            sessions.forEach(session -> sendMessage(session, message));
        }
    }

    private String serializeEvent(GraphEvent event) {
        try {
            Map<String, Object> eventData = Map.of(
                    "type", event.getClass().getSimpleName(),
                    "executionId", event.executionId(),
                    "timestamp", event.timestamp(),
                    "metadata", event.metadata()
            );
            return objectMapper.writeValueAsString(eventData);
        } catch (Exception e) {
            logger.error("Failed to serialize event", e);
            return "{}";
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                logger.error("Failed to send message to session {}", session.getId(), e);
            }
        }
    }
}
