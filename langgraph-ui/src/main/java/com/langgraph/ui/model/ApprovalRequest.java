package com.langgraph.ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {
    private String approvalId;
    private String executionId;
    private String nodeId;
    private String message;
    private Map<String, Object> context;
    private ApprovalStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
    private String response;
}
