package com.gathr.repository;

import com.gathr.entity.ActivityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityMetricsRepository extends JpaRepository<ActivityMetrics, Long> {
}


