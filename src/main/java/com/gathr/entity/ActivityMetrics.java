package com.gathr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "activity_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityMetrics implements Persistable<Long> {

    @Id
    @Column(name = "activity_id")
    private Long activityId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", insertable = false, updatable = false)
    private Activity activity;

    @Column(name = "join_count_1hr")
    private Integer joinCount1hr = 0;

    @Column(name = "total_joins")
    private Integer totalJoins = 0;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "last_join_at")
    private LocalDateTime lastJoinAt;

    @Transient
    private boolean isNew = true;

    @Override
    public Long getId() {
        return activityId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
