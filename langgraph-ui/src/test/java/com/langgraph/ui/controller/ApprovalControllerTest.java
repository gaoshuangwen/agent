package com.langgraph.ui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.langgraph.ui.dto.ApprovalResponse;
import com.langgraph.ui.model.ApprovalRequest;
import com.langgraph.ui.model.ApprovalStatus;
import com.langgraph.ui.service.ApprovalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApprovalController.class)
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApprovalService approvalService;

    @Test
    void shouldGetApproval() throws Exception {
        ApprovalRequest request = ApprovalRequest.builder()
                .approvalId("approval-123")
                .executionId("exec-123")
                .nodeId("node-1")
                .message("Approval needed")
                .status(ApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        when(approvalService.getApproval("approval-123")).thenReturn(Optional.of(request));

        mockMvc.perform(get("/api/approvals/approval-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalId").value("approval-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetPendingApprovals() throws Exception {
        ApprovalRequest request1 = ApprovalRequest.builder()
                .approvalId("approval-1")
                .executionId("exec-1")
                .status(ApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        ApprovalRequest request2 = ApprovalRequest.builder()
                .approvalId("approval-2")
                .executionId("exec-2")
                .status(ApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        when(approvalService.getPendingApprovals()).thenReturn(Arrays.asList(request1, request2));

        mockMvc.perform(get("/api/approvals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldApproveRequest() throws Exception {
        ApprovalResponse response = ApprovalResponse.builder()
                .approved(true)
                .response("Looks good")
                .build();

        ApprovalRequest approvedRequest = ApprovalRequest.builder()
                .approvalId("approval-123")
                .status(ApprovalStatus.APPROVED)
                .response("Looks good")
                .respondedAt(LocalDateTime.now())
                .build();

        when(approvalService.approve(anyString(), anyString())).thenReturn(approvedRequest);

        mockMvc.perform(post("/api/approvals/approval-123/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(response)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldRejectRequest() throws Exception {
        ApprovalResponse response = ApprovalResponse.builder()
                .approved(false)
                .response("Not acceptable")
                .build();

        ApprovalRequest rejectedRequest = ApprovalRequest.builder()
                .approvalId("approval-123")
                .status(ApprovalStatus.REJECTED)
                .response("Not acceptable")
                .respondedAt(LocalDateTime.now())
                .build();

        when(approvalService.reject(anyString(), anyString())).thenReturn(rejectedRequest);

        mockMvc.perform(post("/api/approvals/approval-123/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(response)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
