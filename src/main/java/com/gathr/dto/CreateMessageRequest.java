package com.gathr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMessageRequest {
    
    @NotBlank(message = "Message text is required")
    private String text;
}

