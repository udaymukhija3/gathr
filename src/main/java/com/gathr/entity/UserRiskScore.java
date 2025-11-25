package com.gathr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_risk_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRiskScore {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private Integer baseScore = 0;
    private Integer behaviorScore = 0;
    private Integer reportScore = 0;
    private Integer coordinationScore = 0;
    private Integer totalScore = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


