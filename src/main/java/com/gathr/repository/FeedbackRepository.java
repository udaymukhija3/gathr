package com.gathr.repository;

import com.gathr.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f JOIN FETCH f.activity JOIN FETCH f.user WHERE f.user.id = :userId AND f.activity.id = :activityId")
    Optional<Feedback> findByUserIdAndActivityId(Long userId, Long activityId);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.activity JOIN FETCH f.user WHERE f.activity.id = :activityId ORDER BY f.createdAt DESC")
    List<Feedback> findByActivityId(Long activityId);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.activity JOIN FETCH f.user WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<Feedback> findByUserId(Long userId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.activity.id = :activityId AND f.didMeet = true")
    int countDidMeetByActivityId(Long activityId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.activity.id = :activityId")
    int countByActivityId(Long activityId);

    @Query("SELECT AVG(f.experienceRating) FROM Feedback f WHERE f.activity.id = :activityId AND f.experienceRating IS NOT NULL")
    Double averageRatingByActivityId(Long activityId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.activity.id = :activityId AND f.wouldHangOutAgain = true")
    int countWouldHangOutAgainByActivityId(Long activityId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.activity.id = :activityId AND f.addedToContacts = true")
    int countAddedToContactsByActivityId(Long activityId);

    // User statistics queries
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.user.id = :userId AND f.didMeet = true")
    int countShowUpsByUserId(Long userId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.user.id = :userId AND f.didMeet = false")
    int countNoShowsByUserId(Long userId);

    @Query("SELECT AVG(f.experienceRating) FROM Feedback f WHERE f.user.id = :userId AND f.experienceRating IS NOT NULL")
    Double averageRatingByUserId(Long userId);

    // Feedback about a user (from other participants in the same activities)
    @Query("SELECT AVG(f.experienceRating) FROM Feedback f " +
           "WHERE f.activity.id IN (SELECT p.activity.id FROM Participation p WHERE p.user.id = :userId) " +
           "AND f.user.id != :userId " +
           "AND f.experienceRating IS NOT NULL")
    Double averageRatingReceivedByUserId(Long userId);
}
