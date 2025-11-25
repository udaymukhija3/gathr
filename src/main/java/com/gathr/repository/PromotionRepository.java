package com.gathr.repository;

import com.gathr.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("SELECT p FROM Promotion p WHERE p.hub.id = :hubId AND p.isActive = true")
    List<Promotion> findActiveByHubId(Long hubId);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.startsAt <= :now AND p.expiresAt > :now " +
           "ORDER BY p.startsAt DESC")
    List<Promotion> findCurrentlyActive(LocalDateTime now);

    @Query("SELECT p FROM Promotion p JOIN p.hub h " +
           "WHERE p.isActive = true " +
           "AND p.startsAt <= :now AND p.expiresAt > :now " +
           "AND h.area = :area " +
           "ORDER BY h.partnerTier DESC, p.startsAt DESC")
    List<Promotion> findActiveByArea(String area, LocalDateTime now);

    @Query("SELECT p FROM Promotion p " +
           "WHERE p.isActive = true " +
           "AND p.startsAt <= :now AND p.expiresAt > :now " +
           "AND (p.maxRedemptions IS NULL OR p.currentRedemptions < p.maxRedemptions) " +
           "ORDER BY p.startsAt DESC")
    List<Promotion> findAvailablePromotions(LocalDateTime now);

    @Query(value = "SELECT * FROM promotions p " +
           "WHERE p.is_active = true " +
           "AND p.starts_at <= :now AND p.expires_at > :now " +
           "AND :category = ANY(p.target_categories) " +
           "ORDER BY p.starts_at DESC", nativeQuery = true)
    List<Promotion> findByTargetCategory(String category, LocalDateTime now);

    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.hub.id = :hubId AND p.isActive = true")
    int countActiveByHubId(Long hubId);
}
