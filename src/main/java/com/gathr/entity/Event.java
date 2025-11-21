package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_user_id", columnList = "user_id"),
    @Index(name = "idx_events_activity_id", columnList = "activity_id"),
    @Index(name = "idx_events_event_type", columnList = "event_type"),
    @Index(name = "idx_events_ts", columnList = "ts")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> properties;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime ts;

    @PrePersist
    protected void onCreate() {
        if (ts == null) {
            ts = LocalDateTime.now();
        }
    }
}

