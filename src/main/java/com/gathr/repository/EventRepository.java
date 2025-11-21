package com.gathr.repository;

import com.gathr.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByUserId(Long userId);

    List<Event> findByActivityId(Long activityId);

    List<Event> findByEventType(String eventType);

    @Query("SELECT e FROM Event e WHERE e.userId = :userId AND e.eventType = :eventType ORDER BY e.ts DESC")
    List<Event> findByUserIdAndEventType(@Param("userId") Long userId, @Param("eventType") String eventType);

    @Query("SELECT e FROM Event e WHERE e.activityId = :activityId AND e.eventType = :eventType ORDER BY e.ts DESC")
    List<Event> findByActivityIdAndEventType(@Param("activityId") Long activityId, @Param("eventType") String eventType);

    @Query("SELECT e FROM Event e WHERE e.ts >= :since ORDER BY e.ts DESC")
    List<Event> findRecentEvents(@Param("since") LocalDateTime since);
}

