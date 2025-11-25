package com.gathr.service.feed;

import com.gathr.entity.ActivityMetrics;

import java.util.Optional;

/**
 * Minimal view over {@link com.gathr.repository.ActivityMetricsRepository} so tests can
 * provide simple fakes.
 */
@FunctionalInterface
public interface ActivityMetricsProvider {
    Optional<ActivityMetrics> findById(Long activityId);
}

