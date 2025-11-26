package com.gathr.dto;

import com.gathr.entity.Activity.ActivityCategory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivitySuggestionDto {
    private ActivityCategory category;
    private String title;
    private String suggestedTime; // e.g., "19:00"
    private String reason; // e.g., "Popular on Tuesday evenings"
    private Double confidenceScore; // 0.0 to 1.0
}
