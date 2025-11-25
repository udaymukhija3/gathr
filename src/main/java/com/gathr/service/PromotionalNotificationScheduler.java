package com.gathr.service;

import com.gathr.entity.*;
import com.gathr.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Scheduled service for sending time-based promotional notifications.
 *
 * Key features:
 * - Sends promotions based on day of week and time of day targeting
 * - Respects user notification preferences and quiet hours
 * - Rate limits promotional notifications per user
 * - Targets users based on their interested categories
 */
@Service
public class PromotionalNotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PromotionalNotificationScheduler.class);

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final ParticipationRepository participationRepository;
    private final NotificationService notificationService;

    public PromotionalNotificationScheduler(
            PromotionRepository promotionRepository,
            UserRepository userRepository,
            NotificationPreferenceRepository preferenceRepository,
            ParticipationRepository participationRepository,
            NotificationService notificationService) {
        this.promotionRepository = promotionRepository;
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.participationRepository = participationRepository;
        this.notificationService = notificationService;
    }

    /**
     * Run every 15 minutes to check for promotions to send.
     * Sends targeted promotions based on:
     * - Current day of week
     * - Current time of day
     * - User's interested categories
     * - User's activity history
     */
    @Scheduled(fixedRate = 900000) // Every 15 minutes
    @Transactional
    public void processScheduledPromotions() {
        LocalDateTime now = LocalDateTime.now();
        int dayOfWeek = convertDayOfWeek(now.getDayOfWeek());
        int minutesFromMidnight = now.getHour() * 60 + now.getMinute();

        logger.info("Processing promotional notifications: day={}, time={}", dayOfWeek, minutesFromMidnight);

        // Get all currently valid promotions
        List<Promotion> activePromotions = promotionRepository.findAvailablePromotions(now);

        // Filter by day and time targeting
        List<Promotion> eligiblePromotions = activePromotions.stream()
                .filter(p -> p.isValidForDayAndTime(dayOfWeek, minutesFromMidnight))
                .toList();

        if (eligiblePromotions.isEmpty()) {
            logger.debug("No eligible promotions for current time slot");
            return;
        }

        logger.info("Found {} eligible promotions", eligiblePromotions.size());

        // Get users who opted in for promotional notifications
        List<NotificationPreference> eligiblePrefs = preferenceRepository.findAll().stream()
                .filter(p -> p.getPushEnabled() && p.getPromotionalOffers())
                .filter(p -> !isQuietHours(p, minutesFromMidnight))
                .toList();

        // Process each eligible user
        for (NotificationPreference pref : eligiblePrefs) {
            processUserPromotions(pref.getUser(), eligiblePromotions, now);
        }
    }

    /**
     * Process promotions for a single user.
     */
    private void processUserPromotions(User user, List<Promotion> promotions, LocalDateTime now) {
        // Get user's interested categories
        NotificationPreference prefs = preferenceRepository.findByUserId(user.getId()).orElse(null);
        Set<String> interestedCategories = getInterestedCategories(user, prefs);

        // Find best matching promotion for this user
        Promotion bestPromotion = findBestPromotionForUser(user, promotions, interestedCategories);

        if (bestPromotion != null) {
            try {
                notificationService.sendPromotionalNotification(user, bestPromotion);
                logger.debug("Sent promotional notification to user {} for promotion {}",
                        user.getId(), bestPromotion.getId());
            } catch (Exception e) {
                logger.error("Failed to send promotional notification to user {}: {}",
                        user.getId(), e.getMessage());
            }
        }
    }

    /**
     * Find the best promotion for a user based on their interests and history.
     */
    private Promotion findBestPromotionForUser(User user, List<Promotion> promotions, Set<String> interestedCategories) {
        // Score each promotion for this user
        return promotions.stream()
                .map(p -> new AbstractMap.SimpleEntry<>(p, scorePromotionForUser(p, interestedCategories)))
                .filter(e -> e.getValue() > 0) // Only consider relevant promotions
                .max(Comparator.comparingInt(AbstractMap.SimpleEntry::getValue))
                .map(AbstractMap.SimpleEntry::getKey)
                .orElse(null);
    }

    /**
     * Score a promotion based on user's interests.
     */
    private int scorePromotionForUser(Promotion promotion, Set<String> interestedCategories) {
        int score = 0;

        // Base score for active promotion
        score += 10;

        // Bonus for matching categories
        if (promotion.getTargetCategories() != null) {
            for (String category : promotion.getTargetCategories()) {
                if (interestedCategories.contains(category)) {
                    score += 20;
                }
            }
        }

        // Bonus for premium partner
        if (promotion.getHub().getPartnerTier() == Hub.PartnerTier.PREMIUM) {
            score += 15;
        } else if (promotion.getHub().getPartnerTier() == Hub.PartnerTier.BASIC) {
            score += 5;
        }

        // Penalty for promotions nearing max redemptions
        if (promotion.getMaxRedemptions() != null) {
            double usageRatio = (double) promotion.getCurrentRedemptions() / promotion.getMaxRedemptions();
            if (usageRatio > 0.9) {
                score += 5; // Urgency bonus for limited availability
            }
        }

        return score;
    }

    /**
     * Get user's interested categories from preferences or activity history.
     */
    private Set<String> getInterestedCategories(User user, NotificationPreference prefs) {
        Set<String> categories = new HashSet<>();

        // From explicit preferences
        if (prefs != null && prefs.getInterestedCategories() != null) {
            categories.addAll(Arrays.asList(prefs.getInterestedCategories()));
        }

        // Infer from activity history (categories user has participated in)
        try {
            participationRepository.findByUserId(user.getId()).forEach(participation -> {
                if (participation.getActivity() != null && participation.getActivity().getCategory() != null) {
                    categories.add(participation.getActivity().getCategory().name());
                }
            });
        } catch (Exception e) {
            logger.debug("Could not fetch participation history for user {}", user.getId());
        }

        // Default to all categories if none specified
        if (categories.isEmpty()) {
            categories.addAll(Arrays.asList("SPORTS", "FOOD", "ART", "MUSIC"));
        }

        return categories;
    }

    private boolean isQuietHours(NotificationPreference prefs, int currentMinutes) {
        if (prefs.getQuietHoursStart() == null || prefs.getQuietHoursEnd() == null) {
            return false;
        }
        return prefs.isQuietHoursActive(currentMinutes);
    }

    /**
     * Convert Java DayOfWeek to int (0=Sunday, 1=Monday, etc.)
     */
    private int convertDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SUNDAY -> 0;
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
        };
    }
}
