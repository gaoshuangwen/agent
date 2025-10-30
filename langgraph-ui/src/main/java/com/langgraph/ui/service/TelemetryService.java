package com.langgraph.ui.service;

import com.langgraph.ui.model.ExecutionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TelemetryService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, List<ExecutionEvent>> executionEvents = new ConcurrentHashMap<>();
    
    public TelemetryService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    public void publishEvent(ExecutionEvent event) {
        executionEvents
                .computeIfAbsent(event.getExecutionId(), k -> new ArrayList<>())
                .add(event);
        
        messagingTemplate.convertAndSend("/topic/executions/" + event.getExecutionId(), event);
        messagingTemplate.convertAndSend("/topic/events", event);
        
        log.debug("Published event: {} for execution {}", event.getType(), event.getExecutionId());
    }
    
    public List<ExecutionEvent> getExecutionEvents(String executionId) {
        return new ArrayList<>(executionEvents.getOrDefault(executionId, new ArrayList<>()));
    }
    
    public List<ExecutionEvent> getAllEvents() {
        List<ExecutionEvent> allEvents = new ArrayList<>();
        executionEvents.values().forEach(allEvents::addAll);
        return allEvents;
    }
}
