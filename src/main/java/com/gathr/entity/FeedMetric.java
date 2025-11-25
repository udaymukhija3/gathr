package com.gathr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "hub_id")
    private Long hubId;

    @Column(name = "score")
    private Double score;

    @Column(name = "position")
    private Integer position;

    @Column(name = "action", length = 20)
    private String action;

    @Column(name = "action_timestamp")
    private LocalDateTime actionTimestamp;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (actionTimestamp == null) {
            actionTimestamp = now;
        }
    }
}

