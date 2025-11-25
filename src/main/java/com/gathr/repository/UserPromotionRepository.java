package com.gathr.repository;

import com.gathr.entity.UserPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, Long> {

    Optional<UserPromotion> findByUserIdAndPromotionId(Long userId, Long promotionId);

    @Query("SELECT up FROM UserPromotion up WHERE up.user.id = :userId AND up.savedAt IS NOT NULL")
    List<UserPromotion> findSavedByUserId(Long userId);

    @Query("SELECT up FROM UserPromotion up WHERE up.user.id = :userId AND up.redeemedAt IS NOT NULL")
    List<UserPromotion> findRedeemedByUserId(Long userId);

    @Query("SELECT COUNT(up) FROM UserPromotion up " +
           "WHERE up.user.id = :userId AND up.redeemedAt IS NOT NULL " +
           "AND up.promotion.id = :promotionId")
    int countRedemptionsByUserAndPromotion(Long userId, Long promotionId);

    // Analytics queries
    @Query("SELECT COUNT(up) FROM UserPromotion up WHERE up.promotion.id = :promotionId AND up.viewedAt IS NOT NULL")
    int countViewsByPromotionId(Long promotionId);

    @Query("SELECT COUNT(up) FROM UserPromotion up WHERE up.promotion.id = :promotionId AND up.clickedAt IS NOT NULL")
    int countClicksByPromotionId(Long promotionId);

    @Query("SELECT COUNT(up) FROM UserPromotion up WHERE up.promotion.id = :promotionId AND up.redeemedAt IS NOT NULL")
    int countRedemptionsByPromotionId(Long promotionId);

    // Daily promotion count for rate limiting
    @Query("SELECT COUNT(DISTINCT n.promotion.id) FROM com.gathr.entity.Notification n " +
           "WHERE n.user.id = :userId " +
           "AND n.type = 'PROMOTIONAL' " +
           "AND n.sentAt >= :startOfDay AND n.sentAt < :endOfDay")
    int countPromotionalNotificationsSentToday(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
