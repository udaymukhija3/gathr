package com.gathr.dto;

import com.gathr.entity.InviteToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for InviteToken entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteTokenDto {

    private Long id;
    private Long activityId;
    private String activityTitle;
    private String token;
    private String inviteUrl; // Full shareable URL
    private Long createdById;
    private String createdByName;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Integer useCount;
    private Integer maxUses;
    private Boolean revoked;
    private Boolean isValid;
    private Long invitedUserId;
    private String invitedUserName;
    private String note;

    /**
     * Convert InviteToken entity to DTO
     */
    public static InviteTokenDto fromEntity(InviteToken token) {
        return InviteTokenDto.builder()
            .id(token.getId())
            .activityId(token.getActivity().getId())
            .activityTitle(token.getActivity().getTitle())
            .token(token.getToken())
            .inviteUrl(generateInviteUrl(token.getToken()))
            .createdById(token.getCreatedBy().getId())
            .createdByName(token.getCreatedBy().getName())
            .expiresAt(token.getExpiresAt())
            .createdAt(token.getCreatedAt())
            .useCount(token.getUseCount())
            .maxUses(token.getMaxUses())
            .revoked(token.getRevoked())
            .isValid(token.isValid())
            .invitedUserId(token.getInvitedUser() != null ? token.getInvitedUser().getId() : null)
            .invitedUserName(token.getInvitedUser() != null ? token.getInvitedUser().getName() : null)
            .note(token.getNote())
            .build();
    }

    /**
     * Generate shareable invite URL
     */
    private static String generateInviteUrl(String token) {
        // TODO: Replace with actual app URL from config
        String baseUrl = System.getenv("APP_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://gathr.app";
        }
        return baseUrl + "/invite/" + token;
    }

    /**
     * Convert InviteToken to simplified DTO (without sensitive info)
     */
    public static InviteTokenDto fromEntitySimplified(InviteToken token) {
        return InviteTokenDto.builder()
            .id(token.getId())
            .activityId(token.getActivity().getId())
            .activityTitle(token.getActivity().getTitle())
            .token(token.getToken())
            .inviteUrl(generateInviteUrl(token.getToken()))
            .expiresAt(token.getExpiresAt())
            .isValid(token.isValid())
            .build();
    }
}
