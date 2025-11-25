package com.gathr.repository;

import com.gathr.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserId(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.scheduledFor <= :now ORDER BY n.scheduledFor ASC")
    List<Notification> findPendingScheduled(LocalDateTime now);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.readAt IS NULL")
    int countUnreadByUserId(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.activity.id = :activityId AND n.type = 'ACTIVITY_REMINDER'")
    List<Notification> findActivityReminders(Long activityId);

    // Analytics
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.promotion.id = :promotionId AND n.status = 'SENT'")
    int countSentByPromotionId(Long promotionId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.promotion.id = :promotionId AND n.clickedAt IS NOT NULL")
    int countClickedByPromotionId(Long promotionId);
}
