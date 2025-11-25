package com.gathr.repository;

import com.gathr.entity.FeedMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedMetricRepository extends JpaRepository<FeedMetric, Long> {
}

