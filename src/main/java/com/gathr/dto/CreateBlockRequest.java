package com.gathr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a block
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBlockRequest {

    @NotNull(message = "Blocked user ID is required")
    private Long blockedUserId;

    private String reason; // Optional reason for analytics
}
