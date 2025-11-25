package com.gathr.service.feed;

import com.gathr.entity.ActivityMetrics;
import com.gathr.repository.ActivityMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class DefaultActivityMetricsProvider implements ActivityMetricsProvider {

    private final ActivityMetricsRepository repository;

    @Override
    public Optional<ActivityMetrics> findById(Long activityId) {
        return repository.findById(activityId);
    }
}

