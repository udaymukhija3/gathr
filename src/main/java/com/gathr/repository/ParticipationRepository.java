package com.gathr.repository;

import com.gathr.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    Optional<Participation> findByUserIdAndActivityId(Long userId, Long activityId);
    boolean existsByUserIdAndActivityId(Long userId, Long activityId);
    
    @Query("SELECT p.activity.id FROM Participation p WHERE p.user.id = :userId AND p.activity.id IN :activityIds")
    List<Long> findActivityIdsByUserIdAndActivityIds(@Param("userId") Long userId, @Param("activityIds") List<Long> activityIds);
    Integer countByActivityIdAndStatus(Long activityId, Participation.ParticipationStatus status);
    long countByUserId(Long userId);

    @Query("SELECT COUNT(p) FROM Participation p WHERE p.activity.id = :activityId AND p.status IN :statuses")
    Integer countByActivityIdAndStatusIn(@Param("activityId") Long activityId, @Param("statuses") List<Participation.ParticipationStatus> statuses);

    @Query("SELECT p FROM Participation p JOIN FETCH p.user WHERE p.activity.id = :activityId AND p.status = :status")
    List<Participation> findByActivityIdAndStatus(
            @Param("activityId") Long activityId,
            @Param("status") Participation.ParticipationStatus status);

    @Query("SELECT p FROM Participation p JOIN FETCH p.activity WHERE p.user.id = :userId")
    List<Participation> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Participation p WHERE p.user.id = :userId AND p.activity.hub.id = :hubId")
    long countByUserIdAndHubId(@Param("userId") Long userId, @Param("hubId") Long hubId);

    @Query("SELECT MAX(p.joinedTs) FROM Participation p WHERE p.user.id = :userId")
    LocalDateTime findLatestParticipationTs(@Param("userId") Long userId);

    @Query("""
            SELECT p.activity.category, COUNT(p)
            FROM Participation p
            WHERE p.user.id = :userId
              AND p.status = com.gathr.entity.Participation$ParticipationStatus.CONFIRMED
            GROUP BY p.activity.category
            """)
    List<Object[]> findConfirmedCountByCategory(@Param("userId") Long userId);

    @Query("""
            SELECT AVG(EXTRACT(HOUR FROM a.startTime))
            FROM Participation p
            JOIN p.activity a
            WHERE p.user.id = :userId
              AND p.status = com.gathr.entity.Participation$ParticipationStatus.CONFIRMED
            """)
    Double findAverageStartHourForUser(@Param("userId") Long userId);
}

