package com.langgraph.ui.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {
    @NotNull(message = "Approval decision is required")
    private Boolean approved;
    
    @NotBlank(message = "Response message is required")
    private String response;
}
