package com.gathr.repository;

import com.gathr.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    Optional<Participation> findByUserIdAndActivityId(Long userId, Long activityId);
    boolean existsByUserIdAndActivityId(Long userId, Long activityId);
    Integer countByActivityIdAndStatus(Long activityId, Participation.ParticipationStatus status);
}

