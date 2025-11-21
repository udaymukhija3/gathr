package com.gathr.repository;

import com.gathr.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.user " +
           "LEFT JOIN FETCH m.activity " +
           "WHERE m.activity.id = :activityId ORDER BY m.createdAt ASC")
    List<Message> findByActivityIdOrderByCreatedAtAsc(@Param("activityId") Long activityId);

    @Query("SELECT m FROM Message m " +
           "JOIN m.activity a " +
           "WHERE m.createdAt < :threshold " +
           "AND a.endTime < :threshold")
    List<Message> findExpiredMessages(@Param("threshold") java.time.LocalDateTime threshold);
}

