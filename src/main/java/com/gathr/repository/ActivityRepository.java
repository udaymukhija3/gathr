package com.gathr.repository;

import com.gathr.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

        @Query("SELECT a FROM Activity a " +
                        "LEFT JOIN FETCH a.hub " +
                        "LEFT JOIN FETCH a.createdBy " +
                        "WHERE a.hub.id = :hubId " +
                        "AND DATE(a.startTime) = :date " +
                        "AND a.status = 'SCHEDULED' " +
                        "ORDER BY a.startTime ASC")
        List<Activity> findByHubIdAndDate(@Param("hubId") Long hubId, @Param("date") LocalDate date);

        @Query("SELECT DISTINCT a FROM Activity a " +
                        "LEFT JOIN FETCH a.hub " +
                        "LEFT JOIN FETCH a.createdBy " +
                        "WHERE a.startTime BETWEEN :startWindow AND :endWindow " +
                        "AND a.status = 'SCHEDULED'")
        List<Activity> findActivitiesStartingBetween(
                        @Param("startWindow") LocalDateTime startWindow,
                        @Param("endWindow") LocalDateTime endWindow);

        @Query("SELECT a FROM Activity a WHERE a.hub.id = :hubId AND a.status = :status")
        List<Activity> findByHubIdAndStatus(
                        @Param("hubId") Long hubId,
                        @Param("status") Activity.ActivityStatus status);

        /**
         * Count activities created by a user after a certain time (for spam detection).
         */
        @Query("SELECT COUNT(a) FROM Activity a WHERE a.createdBy.id = :userId AND a.createdAt > :since")
        int countByCreatedByIdAndCreatedAtAfter(
                        @Param("userId") Long userId,
                        @Param("since") LocalDateTime since);
}
