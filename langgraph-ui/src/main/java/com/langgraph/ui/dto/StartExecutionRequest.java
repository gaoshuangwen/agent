package com.langgraph.ui.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartExecutionRequest {
    @NotBlank(message = "Graph ID is required")
    private String graphId;
    
    private Map<String, Object> input;
}
