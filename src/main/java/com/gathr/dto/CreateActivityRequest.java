package com.gathr.dto;

import com.gathr.entity.Activity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateActivityRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotNull(message = "Hub ID is required")
    private Long hubId;
    
    @NotNull(message = "Category is required")
    private Activity.ActivityCategory category;
    
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}

