package com.gathr.repository;

import com.gathr.entity.UserRiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRiskScoreRepository extends JpaRepository<UserRiskScore, Long> {

    /**
     * Find users with high risk scores (for daily re-evaluation).
     */
    List<UserRiskScore> findByTotalScoreGreaterThan(int threshold);

    /**
     * Find users at or above a certain risk threshold.
     */
    List<UserRiskScore> findByTotalScoreGreaterThanEqual(int threshold);
}


