package com.gathr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityMetrics {

    @Id
    @Column(name = "activity_id")
    private Long activityId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "join_count_1hr")
    private Integer joinCount1hr = 0;

    @Column(name = "total_joins")
    private Integer totalJoins = 0;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "last_join_at")
    private LocalDateTime lastJoinAt;
}


