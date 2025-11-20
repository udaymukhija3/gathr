package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * AuditLog entity - tracks admin and system actions for compliance and debugging.
 * Used to maintain audit trail of moderator actions, bans, etc.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_actor_id", columnList = "actor_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_entity", columnList = "entity"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User or admin who performed the action (null for system actions)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /**
     * Type of action performed
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType action;

    /**
     * Entity type that was acted upon
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entity;

    /**
     * ID of the entity that was acted upon
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Additional context or details about the action (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * IP address of the actor (for security)
     */
    @Column(length = 50)
    private String ipAddress;

    /**
     * User agent of the actor
     */
    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    /**
     * Types of actions that can be audited
     */
    public enum ActionType {
        // User actions
        USER_CREATED,
        USER_BANNED,
        USER_UNBANNED,
        USER_VERIFIED,

        // Activity actions
        ACTIVITY_CREATED,
        ACTIVITY_DELETED,
        ACTIVITY_MODIFIED,

        // Moderation actions
        REPORT_CREATED,
        REPORT_REVIEWED,
        REPORT_RESOLVED,
        BLOCK_CREATED,
        BLOCK_REMOVED,

        // Message actions
        MESSAGE_DELETED,
        MESSAGE_FLAGGED,

        // Invite actions
        INVITE_CREATED,
        INVITE_REVOKED,

        // System actions
        AUTO_BAN_TRIGGERED,
        PASSWORD_RESET,
        DATA_EXPORTED,
        DATA_DELETED
    }

    /**
     * Types of entities that can be audited
     */
    public enum EntityType {
        USER,
        ACTIVITY,
        MESSAGE,
        REPORT,
        BLOCK,
        INVITE_TOKEN,
        PARTICIPATION,
        HUB
    }

    /**
     * Create an audit log entry
     */
    public static AuditLog create(User actor, ActionType action, EntityType entity, Long entityId, String details) {
        return AuditLog.builder()
            .actor(actor)
            .action(action)
            .entity(entity)
            .entityId(entityId)
            .details(details)
            .build();
    }

    /**
     * Create a system audit log entry (no actor)
     */
    public static AuditLog createSystem(ActionType action, EntityType entity, Long entityId, String details) {
        return create(null, action, entity, entityId, details);
    }
}
