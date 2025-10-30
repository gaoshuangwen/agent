package com.langgraph.ui.service;

import com.langgraph.ui.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ApprovalService {
    
    private final Map<String, ApprovalRequest> approvalRequests = new ConcurrentHashMap<>();
    private final TelemetryService telemetryService;
    
    public ApprovalService(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }
    
    public ApprovalRequest requestApproval(String executionId, String nodeId, String message, Map<String, Object> context) {
        String approvalId = UUID.randomUUID().toString();
        
        ApprovalRequest request = ApprovalRequest.builder()
                .approvalId(approvalId)
                .executionId(executionId)
                .nodeId(nodeId)
                .message(message)
                .context(context)
                .status(ApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();
        
        approvalRequests.put(approvalId, request);
        
        ExecutionEvent event = ExecutionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .executionId(executionId)
                .type(EventType.APPROVAL_REQUESTED)
                .nodeId(nodeId)
                .timestamp(LocalDateTime.now())
                .message("Approval requested: " + message)
                .metadata(Map.of("approvalId", approvalId))
                .build();
        
        telemetryService.publishEvent(event);
        
        log.info("Approval requested: {} for execution {} at node {}", approvalId, executionId, nodeId);
        
        return request;
    }
    
    public Optional<ApprovalRequest> getApproval(String approvalId) {
        return Optional.ofNullable(approvalRequests.get(approvalId));
    }
    
    public List<ApprovalRequest> getPendingApprovals() {
        return approvalRequests.values().stream()
                .filter(req -> req.getStatus() == ApprovalStatus.PENDING)
                .toList();
    }
    
    public ApprovalRequest approve(String approvalId, String response) {
        ApprovalRequest request = approvalRequests.get(approvalId);
        if (request == null) {
            throw new IllegalArgumentException("Approval request not found: " + approvalId);
        }
        
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request already processed: " + approvalId);
        }
        
        request.setStatus(ApprovalStatus.APPROVED);
        request.setResponse(response);
        request.setRespondedAt(LocalDateTime.now());
        
        ExecutionEvent event = ExecutionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .executionId(request.getExecutionId())
                .type(EventType.APPROVAL_GRANTED)
                .nodeId(request.getNodeId())
                .timestamp(LocalDateTime.now())
                .message("Approval granted: " + response)
                .metadata(Map.of("approvalId", approvalId))
                .build();
        
        telemetryService.publishEvent(event);
        
        log.info("Approval granted: {}", approvalId);
        
        return request;
    }
    
    public ApprovalRequest reject(String approvalId, String response) {
        ApprovalRequest request = approvalRequests.get(approvalId);
        if (request == null) {
            throw new IllegalArgumentException("Approval request not found: " + approvalId);
        }
        
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request already processed: " + approvalId);
        }
        
        request.setStatus(ApprovalStatus.REJECTED);
        request.setResponse(response);
        request.setRespondedAt(LocalDateTime.now());
        
        ExecutionEvent event = ExecutionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .executionId(request.getExecutionId())
                .type(EventType.APPROVAL_DENIED)
                .nodeId(request.getNodeId())
                .timestamp(LocalDateTime.now())
                .message("Approval denied: " + response)
                .metadata(Map.of("approvalId", approvalId))
                .build();
        
        telemetryService.publishEvent(event);
        
        log.info("Approval rejected: {}", approvalId);
        
        return request;
    }
}
