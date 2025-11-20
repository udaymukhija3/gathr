package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Block entity - represents a user blocking another user.
 * When user A blocks user B:
 * - B cannot see A's activities
 * - B cannot join activities A has joined
 * - B cannot send messages to A
 * - A won't see B's activities
 */
@Entity
@Table(name = "blocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who initiated the block
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    /**
     * User who is being blocked
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional reason for blocking (for internal analytics)
     */
    @Column(length = 500)
    private String reason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Check if blocker and blocked are the same user (validation)
     */
    public boolean isSelfBlock() {
        return blocker != null && blocked != null
            && blocker.getId().equals(blocked.getId());
    }
}
