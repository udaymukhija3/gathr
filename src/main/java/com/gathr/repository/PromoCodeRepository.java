package com.gathr.repository;

import com.gathr.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    Optional<PromoCode> findByCode(String code);

    @Query("SELECT pc FROM PromoCode pc WHERE pc.promotion.id = :promotionId")
    List<PromoCode> findByPromotionId(Long promotionId);

    @Query("SELECT pc FROM PromoCode pc WHERE pc.code = :code " +
           "AND (pc.expiresAt IS NULL OR pc.expiresAt > :now) " +
           "AND (pc.maxUses IS NULL OR pc.currentUses < pc.maxUses)")
    Optional<PromoCode> findValidByCode(String code, LocalDateTime now);
}
