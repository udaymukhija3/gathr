package com.gathr.repository;

import com.gathr.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    Optional<Participation> findByUserIdAndActivityId(Long userId, Long activityId);
    boolean existsByUserIdAndActivityId(Long userId, Long activityId);
    Integer countByActivityIdAndStatus(Long activityId, Participation.ParticipationStatus status);
    
    @Query("SELECT COUNT(p) FROM Participation p WHERE p.activity.id = :activityId AND p.status IN :statuses")
    Integer countByActivityIdAndStatusIn(@Param("activityId") Long activityId, @Param("statuses") List<Participation.ParticipationStatus> statuses);
}

