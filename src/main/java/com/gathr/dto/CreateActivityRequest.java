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
    
    private Long hubId;
    
    @NotNull(message = "Category is required")
    private Activity.ActivityCategory category;
    
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
    
    private Boolean isInviteOnly;
    private Integer maxMembers;

    private String placeId;
    private String placeName;
    private String placeAddress;
    private Double latitude;
    private Double longitude;

    public boolean hasCustomLocation() {
        return hubId == null;
    }

    public boolean hasValidCustomLocation() {
        return placeName != null && !placeName.isBlank()
                && latitude != null && longitude != null;
    }
}

