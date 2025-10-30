package com.langgraph.ui.controller;

import com.langgraph.ui.dto.ApprovalResponse;
import com.langgraph.ui.model.ApprovalRequest;
import com.langgraph.ui.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/approvals")
@CrossOrigin(origins = "*")
public class ApprovalController {
    
    private final ApprovalService approvalService;
    
    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }
    
    @GetMapping("/{approvalId}")
    public ResponseEntity<ApprovalRequest> getApproval(@PathVariable String approvalId) {
        return approvalService.getApproval(approvalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<ApprovalRequest>> getPendingApprovals() {
        List<ApprovalRequest> requests = approvalService.getPendingApprovals();
        return ResponseEntity.ok(requests);
    }
    
    @PostMapping("/{approvalId}/approve")
    public ResponseEntity<ApprovalRequest> approve(
            @PathVariable String approvalId,
            @Valid @RequestBody ApprovalResponse response) {
        
        try {
            ApprovalRequest request;
            if (response.getApproved()) {
                request = approvalService.approve(approvalId, response.getResponse());
            } else {
                request = approvalService.reject(approvalId, response.getResponse());
            }
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error processing approval: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
