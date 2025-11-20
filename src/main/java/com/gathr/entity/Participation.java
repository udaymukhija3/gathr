package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "participations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "activity_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipationStatus status;

    @Column(name = "joined_ts")
    private LocalDateTime joinedTs;

    @Column(name = "left_ts")
    private LocalDateTime leftTs;

    @Column(name = "brings_plus_one", nullable = false)
    private Boolean bringsPlusOne = false;

    @PrePersist
    protected void onCreate() {
        if (joinedTs == null) {
            joinedTs = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (status == ParticipationStatus.LEFT && leftTs == null) {
            leftTs = LocalDateTime.now();
        }
    }

    public enum ParticipationStatus {
        INTERESTED, CONFIRMED, LEFT
    }
}

