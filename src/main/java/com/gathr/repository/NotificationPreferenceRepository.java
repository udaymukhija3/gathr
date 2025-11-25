package com.gathr.repository;

import com.gathr.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserId(Long userId);

    @Query("SELECT p FROM NotificationPreference p WHERE p.user.id = :userId AND p.pushEnabled = true")
    Optional<NotificationPreference> findEnabledByUserId(Long userId);

    @Query("SELECT p FROM NotificationPreference p WHERE p.user.id = :userId AND p.promotionalOffers = true")
    Optional<NotificationPreference> findPromotionalEnabledByUserId(Long userId);
}
