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
@Table(name = "reports", indexes = {
    @Index(name = "idx_target_user", columnList = "target_user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
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
     * Optional: specific message being reported (if report is about a message)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    /**
     * Optional: activity context where report occurred
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    /**
     * Reason category for the report
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    /**
     * Additional details from reporter (free text)
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * Current status of the report
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the report was reviewed by a moderator
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * Admin/moderator who reviewed the report
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /**
     * Admin notes/actions taken
     */
    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }

    /**
     * Reason categories for reports
     */
    public enum ReportReason {
        HARASSMENT("Harassment or bullying"),
        INAPPROPRIATE_CONTENT("Inappropriate or offensive content"),
        SPAM("Spam or promotional content"),
        FAKE_PROFILE("Fake or impersonation profile"),
        SAFETY_CONCERN("Safety concern or threatening behavior"),
        NO_SHOW("Didn't show up to meetup"),
        OTHER("Other");

        private final String displayName;

        ReportReason(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Report processing status
     */
    public enum ReportStatus {
        PENDING("Awaiting review"),
        UNDER_REVIEW("Being reviewed by moderator"),
        RESOLVED_ACTION_TAKEN("Resolved - action taken"),
        RESOLVED_NO_ACTION("Resolved - no action needed"),
        DISMISSED("Dismissed as invalid");

        private final String displayName;

        ReportStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Mark report as reviewed
     */
    public void markReviewed(User reviewer, String notes) {
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewer;
        this.adminNotes = notes;
    }
}
