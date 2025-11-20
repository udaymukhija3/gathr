package com.gathr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an invite token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInviteTokenRequest {

    @NotNull(message = "Activity ID is required")
    private Long activityId;

    private Integer expiresInHours; // Optional: hours until expiration (default: 48)

    private Integer maxUses; // Optional: max times token can be used (null = unlimited)

    private Long invitedUserId; // Optional: specific user invite (null = anyone with link)

    private String note; // Optional note about this invite
}
