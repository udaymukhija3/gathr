package com.gathr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {

    @NotNull(message = "Target user ID is required")
    private Long targetUserId;

    private Long messageId; // Optional: specific message being reported

    private Long activityId; // Optional: activity context

    @NotNull(message = "Reason is required")
    private String reason; // HARASSMENT, INAPPROPRIATE_CONTENT, SPAM, etc.

    private String details; // Optional: additional details from reporter
}
