package com.gathr.service;

import com.gathr.entity.Activity;
import com.gathr.entity.ActivityMetrics;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.ActivityMetricsRepository;
import com.gathr.repository.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Lightweight metrics service to keep track of joins/views per activity.
 * Phase 3 focuses on recording joins; views and 1hr windows can be refined later.
 */
@Service
public class ActivityMetricsService {

    private final ActivityMetricsRepository activityMetricsRepository;
    private final ActivityRepository activityRepository;

    public ActivityMetricsService(ActivityMetricsRepository activityMetricsRepository,
                                  ActivityRepository activityRepository) {
        this.activityMetricsRepository = activityMetricsRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public void recordJoin(Long activityId) {
        ActivityMetrics metrics = activityMetricsRepository.findById(activityId)
                .orElseGet(() -> createMetrics(activityId));

        if (metrics.getTotalJoins() == null) {
            metrics.setTotalJoins(0);
        }
        metrics.setTotalJoins(metrics.getTotalJoins() + 1);
        metrics.setLastJoinAt(LocalDateTime.now());

        activityMetricsRepository.save(metrics);
    }

    @Transactional
    public void recordView(Long activityId) {
        ActivityMetrics metrics = activityMetricsRepository.findById(activityId)
                .orElseGet(() -> createMetrics(activityId));

        if (metrics.getViewCount() == null) {
            metrics.setViewCount(0);
        }
        metrics.setViewCount(metrics.getViewCount() + 1);
        activityMetricsRepository.save(metrics);
    }

    private ActivityMetrics createMetrics(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));
        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setActivity(activity);
        metrics.setActivityId(activity.getId());
        metrics.setJoinCount1hr(0);
        metrics.setTotalJoins(0);
        metrics.setViewCount(0);
        metrics.setLastJoinAt(null);
        return metrics;
    }
}


