package com.gathr.repository;

import com.gathr.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f WHERE f.user.id = :userId AND f.activity.id = :activityId")
    Optional<Feedback> findByUserIdAndActivityId(Long userId, Long activityId);

    @Query("SELECT f FROM Feedback f WHERE f.activity.id = :activityId")
    List<Feedback> findByActivityId(Long activityId);

    @Query("SELECT f FROM Feedback f WHERE f.user.id = :userId")
    List<Feedback> findByUserId(Long userId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.activity.id = :activityId AND f.didMeet = true")
    int countDidMeetByActivityId(Long activityId);
}
