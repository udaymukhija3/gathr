package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Report entity - represents a user report for harassment, spam, or inappropriate behavior.
 * Used for moderation and auto-ban logic.
 */
@Entity
@Table(name = "report", indexes = {
    @Index(name = "idx_report_target_user", columnList = "target_user_id"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who submitted the report
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /**
     * User being reported
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    /**
     * Optional: activity context where report occurred
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    /**
     * Reason for the report (free text, max 512 chars)
     */
    @Column(nullable = false, length = 512)
    private String reason;

    /**
     * Current status of the report (default: OPEN)
     */
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "OPEN";
        }
    }
}
