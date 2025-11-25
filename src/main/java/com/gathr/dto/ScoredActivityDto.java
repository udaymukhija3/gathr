package com.gathr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Wrapper DTO for a scored activity in the personalized feed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredActivityDto {
    private ActivityDto activity;
    private Double score;
    private String primaryReason;
    private String secondaryReason;
    private Integer mutualCount;
    private Integer spotsRemaining;
    private Map<String, Object> metadata;
    private boolean diversityPenaltyApplied;
}


