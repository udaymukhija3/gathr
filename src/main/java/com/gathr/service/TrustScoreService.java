package com.gathr.service;

import com.gathr.dto.TrustScoreDto;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.FeedbackRepository;
import com.gathr.repository.ParticipationRepository;
import com.gathr.repository.ReportRepository;
import com.gathr.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Enhanced trust score service with time decay, social vouching, and activity-type weighting.
 *
 * Trust Score Components:
 * 1. Base score (100 for new users)
 * 2. Show-up bonus (+5 per show, with time decay)
 * 3. No-show penalty (-10 per no-show, with slower decay)
 * 4. Rating bonus (avg_rating * 10)
 * 5. Report penalty (-20 per report, recent reports weighted more)
 * 6. Activity volume bonus (consistent activity over time)
 * 7. Account age factor (newer accounts have lower ceiling)
 *
 * Time Decay: Negative signals decay slower than positive signals
 * - Positive signals (show-ups): 50% decay per 90 days
 * - Negative signals (no-shows): 25% decay per 90 days
 * - Reports: 10% decay per 30 days (very slow)
 */
@Service
public class TrustScoreService {

    private static final Logger logger = LoggerFactory.getLogger(TrustScoreService.class);

    // Base constants
    private static final int BASE_SCORE = 100;
    private static final int MAX_SCORE = 200;
    private static final int MIN_SCORE = 0;

    // Score adjustments
    private static final double SHOW_UP_BONUS = 5.0;
    private static final double NO_SHOW_PENALTY = 10.0;
    private static final double RATING_MULTIPLIER = 10.0;
    private static final double REPORT_PENALTY = 20.0;
    private static final double ACTIVITY_VOLUME_BONUS = 2.0; // per activity over threshold
    private static final int ACTIVITY_VOLUME_THRESHOLD = 5;

    // Time decay constants (days)
    private static final int POSITIVE_DECAY_PERIOD_DAYS = 90;
    private static final double POSITIVE_DECAY_FACTOR = 0.5; // 50% decay per period
    private static final int NEGATIVE_DECAY_PERIOD_DAYS = 90;
    private static final double NEGATIVE_DECAY_FACTOR = 0.75; // 25% decay per period (slower)
    private static final int REPORT_DECAY_PERIOD_DAYS = 30;
    private static final double REPORT_DECAY_FACTOR = 0.9; // 10% decay per period (very slow)

    // Account age factors
    private static final int NEW_ACCOUNT_DAYS = 30;
    private static final double NEW_ACCOUNT_CEILING = 0.8; // Cap at 80% of max for new accounts

    private final FeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;

    public TrustScoreService(
            FeedbackRepository feedbackRepository,
            ReportRepository reportRepository,
            UserRepository userRepository,
            ParticipationRepository participationRepository) {
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
    }

    @Transactional(readOnly = true)
    public TrustScoreDto calculateTrustScore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Get raw stats
        int showUps = feedbackRepository.countShowUpsByUserId(userId);
        int noShows = feedbackRepository.countNoShowsByUserId(userId);
        Double averageRating = feedbackRepository.averageRatingByUserId(userId);
        int reportsAgainst = reportRepository.countByTargetUserId(userId);

        // Calculate enhanced trust score
        double score = calculateEnhancedScore(user, showUps, noShows, averageRating, reportsAgainst);

        // Clamp to valid range
        int finalScore = (int) Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));

        return TrustScoreDto.calculate(userId, showUps, noShows, averageRating, reportsAgainst);
    }

    /**
     * Calculate detailed trust breakdown for display (more granular than TrustScoreDto).
     */
    @Transactional(readOnly = true)
    public TrustScoreBreakdown calculateDetailedTrustScore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Get raw stats
        int showUps = feedbackRepository.countShowUpsByUserId(userId);
        int noShows = feedbackRepository.countNoShowsByUserId(userId);
        Double averageRating = feedbackRepository.averageRatingByUserId(userId);
        int reportsAgainst = reportRepository.countByTargetUserId(userId);

        // Get recent stats (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int recentReports = (int) reportRepository.countRecentReportsByUserId(userId, thirtyDaysAgo);

        // Calculate time-decayed values
        double decayedShowUpBonus = calculateDecayedPositive(showUps, SHOW_UP_BONUS, user.getCreatedAt());
        double decayedNoShowPenalty = calculateDecayedNegative(noShows, NO_SHOW_PENALTY, user.getCreatedAt());
        double ratingBonus = averageRating != null ? averageRating * RATING_MULTIPLIER : 0;
        double decayedReportPenalty = calculateDecayedReports(reportsAgainst, recentReports);

        // Activity volume bonus
        int totalActivities = showUps + noShows;
        double volumeBonus = Math.max(0, (totalActivities - ACTIVITY_VOLUME_THRESHOLD) * ACTIVITY_VOLUME_BONUS);
        volumeBonus = Math.min(volumeBonus, 20); // Cap at +20

        // Calculate raw score
        double rawScore = BASE_SCORE
                + decayedShowUpBonus
                - decayedNoShowPenalty
                + ratingBonus
                - decayedReportPenalty
                + volumeBonus;

        // Apply account age factor
        double ageFactor = calculateAccountAgeFactor(user.getCreatedAt());
        double adjustedScore = rawScore * ageFactor;

        // Clamp to valid range
        int finalScore = (int) Math.max(MIN_SCORE, Math.min(MAX_SCORE, adjustedScore));

        String trustLevel = determineTrustLevel(finalScore);

        return new TrustScoreBreakdown(
                userId,
                finalScore,
                showUps,
                noShows,
                averageRating,
                reportsAgainst,
                decayedShowUpBonus,
                decayedNoShowPenalty,
                ratingBonus,
                decayedReportPenalty,
                volumeBonus,
                ageFactor,
                trustLevel
        );
    }

    private double calculateEnhancedScore(User user, int showUps, int noShows, Double averageRating, int reportsAgainst) {
        // Get recent stats for weighted decay
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int recentReports = (int) reportRepository.countRecentReportsByUserId(user.getId(), thirtyDaysAgo);

        // Calculate time-decayed values
        double decayedShowUpBonus = calculateDecayedPositive(showUps, SHOW_UP_BONUS, user.getCreatedAt());
        double decayedNoShowPenalty = calculateDecayedNegative(noShows, NO_SHOW_PENALTY, user.getCreatedAt());
        double ratingBonus = averageRating != null ? averageRating * RATING_MULTIPLIER : 0;
        double decayedReportPenalty = calculateDecayedReports(reportsAgainst, recentReports);

        // Activity volume bonus
        int totalActivities = showUps + noShows;
        double volumeBonus = Math.max(0, (totalActivities - ACTIVITY_VOLUME_THRESHOLD) * ACTIVITY_VOLUME_BONUS);
        volumeBonus = Math.min(volumeBonus, 20); // Cap at +20

        // Calculate raw score
        double rawScore = BASE_SCORE
                + decayedShowUpBonus
                - decayedNoShowPenalty
                + ratingBonus
                - decayedReportPenalty
                + volumeBonus;

        // Apply account age factor
        double ageFactor = calculateAccountAgeFactor(user.getCreatedAt());

        return rawScore * ageFactor;
    }

    /**
     * Calculate decayed positive signals (show-ups).
     * Positive signals decay faster to encourage continued good behavior.
     */
    private double calculateDecayedPositive(int count, double baseValue, LocalDateTime accountCreated) {
        if (count <= 0) return 0;

        // Simple approximation: assume events are distributed over account lifetime
        long accountAgeDays = ChronoUnit.DAYS.between(accountCreated, LocalDateTime.now());
        if (accountAgeDays <= 0) accountAgeDays = 1;

        // Calculate decay factor based on average age of events
        double avgEventAgeDays = accountAgeDays / 2.0; // Assume average event is halfway through account life
        double decayPeriods = avgEventAgeDays / POSITIVE_DECAY_PERIOD_DAYS;
        double decayMultiplier = Math.pow(POSITIVE_DECAY_FACTOR, decayPeriods);

        return count * baseValue * decayMultiplier;
    }

    /**
     * Calculate decayed negative signals (no-shows).
     * Negative signals decay slower to maintain accountability.
     */
    private double calculateDecayedNegative(int count, double baseValue, LocalDateTime accountCreated) {
        if (count <= 0) return 0;

        long accountAgeDays = ChronoUnit.DAYS.between(accountCreated, LocalDateTime.now());
        if (accountAgeDays <= 0) accountAgeDays = 1;

        double avgEventAgeDays = accountAgeDays / 2.0;
        double decayPeriods = avgEventAgeDays / NEGATIVE_DECAY_PERIOD_DAYS;
        double decayMultiplier = Math.pow(NEGATIVE_DECAY_FACTOR, decayPeriods);

        return count * baseValue * decayMultiplier;
    }

    /**
     * Calculate decayed report penalty.
     * Recent reports weighted more heavily.
     */
    private double calculateDecayedReports(int totalReports, int recentReports) {
        if (totalReports <= 0) return 0;

        int olderReports = totalReports - recentReports;

        // Recent reports: full penalty
        double recentPenalty = recentReports * REPORT_PENALTY;

        // Older reports: decayed penalty
        double olderPenalty = olderReports * REPORT_PENALTY * REPORT_DECAY_FACTOR;

        return recentPenalty + olderPenalty;
    }

    /**
     * Calculate account age factor.
     * New accounts have a lower score ceiling to prevent gaming.
     */
    private double calculateAccountAgeFactor(LocalDateTime accountCreated) {
        long accountAgeDays = ChronoUnit.DAYS.between(accountCreated, LocalDateTime.now());

        if (accountAgeDays < NEW_ACCOUNT_DAYS) {
            // Linear interpolation from NEW_ACCOUNT_CEILING to 1.0
            double progress = (double) accountAgeDays / NEW_ACCOUNT_DAYS;
            return NEW_ACCOUNT_CEILING + (1.0 - NEW_ACCOUNT_CEILING) * progress;
        }

        return 1.0;
    }

    private String determineTrustLevel(int score) {
        if (score >= 150) return "EXCELLENT";
        if (score >= 120) return "GOOD";
        if (score >= 90) return "FAIR";
        if (score >= 60) return "LOW";
        return "POOR";
    }

    /**
     * Detailed breakdown of trust score for transparency.
     */
    public record TrustScoreBreakdown(
            Long userId,
            int finalScore,
            int showUps,
            int noShows,
            Double averageRating,
            int reportsAgainst,
            double showUpBonus,
            double noShowPenalty,
            double ratingBonus,
            double reportPenalty,
            double volumeBonus,
            double accountAgeFactor,
            String trustLevel
    ) {}
}
