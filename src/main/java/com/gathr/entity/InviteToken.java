package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * InviteToken entity - represents an invitation to an invite-only activity.
 * Tokens are generated when users share invite links.
 */
@Entity
@Table(name = "invite_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_activity_id", columnList = "activity_id"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Activity this invite is for
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    /**
     * Unique invite token (used in shareable links)
     */
    @Column(nullable = false, unique = true, length = 100)
    private String token;

    /**
     * User who created this invite
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * When this invite expires (typically 24-48 hours)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * How many times this token has been used
     */
    @Column(name = "use_count", nullable = false)
    private Integer useCount;

    /**
     * Maximum number of times this token can be used (null = unlimited)
     */
    @Column(name = "max_uses")
    private Integer maxUses;

    /**
     * Whether this token has been revoked
     */
    @Column(nullable = false)
    private Boolean revoked;

    /**
     * Optional: specific user this invite is for (null = anyone with link can use)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user_id")
    private User invitedUser;

    /**
     * Optional note about this invite
     */
    @Column(length = 500)
    private String note;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (token == null) {
            token = generateToken();
        }
        if (useCount == null) {
            useCount = 0;
        }
        if (revoked == null) {
            revoked = false;
        }
        if (expiresAt == null) {
            // Default: expires in 48 hours
            expiresAt = LocalDateTime.now().plusHours(48);
        }
    }

    /**
     * Generate a unique, URL-safe token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Check if token is still valid
     */
    public boolean isValid() {
        // Token is valid if:
        // 1. Not revoked
        // 2. Not expired
        // 3. Under max uses (if max uses is set)
        if (revoked) {
            return false;
        }
        if (LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        if (maxUses != null && useCount >= maxUses) {
            return false;
        }
        return true;
    }

    /**
     * Increment use count
     */
    public void incrementUseCount() {
        this.useCount++;
    }

    /**
     * Revoke this token
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * Check if token is for a specific user
     */
    public boolean isForSpecificUser() {
        return invitedUser != null;
    }

    /**
     * Check if a user is allowed to use this token
     */
    public boolean canBeUsedBy(User user) {
        if (!isValid()) {
            return false;
        }
        if (invitedUser == null) {
            return true; // Anyone can use
        }
        return invitedUser.getId().equals(user.getId());
    }
}
