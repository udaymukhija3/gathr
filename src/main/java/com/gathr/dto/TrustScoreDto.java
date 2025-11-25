package com.gathr.dto;

/**
 * DTO for user trust score information.
 * Trust score calculation: Base 100 + (show_ups * 5) - (no_shows * 10) + (avg_rating * 10) - (reports * 20)
 */
public record TrustScoreDto(
    Long userId,
    int trustScore,
    int showUps,
    int noShows,
    Double averageRating,
    int reportsAgainst,
    double showUpRate,
    String trustLevel
) {
    private static final int BASE_SCORE = 100;
    private static final int SHOW_UP_BONUS = 5;
    private static final int NO_SHOW_PENALTY = 10;
    private static final int RATING_MULTIPLIER = 10;
    private static final int REPORT_PENALTY = 20;

    public static TrustScoreDto calculate(
            Long userId,
            int showUps,
            int noShows,
            Double averageRating,
            int reportsAgainst
    ) {
        // Calculate trust score
        int score = BASE_SCORE;
        score += showUps * SHOW_UP_BONUS;
        score -= noShows * NO_SHOW_PENALTY;
        if (averageRating != null) {
            score += (int) (averageRating * RATING_MULTIPLIER);
        }
        score -= reportsAgainst * REPORT_PENALTY;

        // Ensure score stays within bounds (0-200)
        score = Math.max(0, Math.min(200, score));

        // Calculate show-up rate
        int totalActivities = showUps + noShows;
        double showUpRate = totalActivities > 0
            ? (double) showUps / totalActivities * 100
            : 100.0; // Default to 100% for new users

        // Determine trust level
        String trustLevel = determineTrustLevel(score);

        return new TrustScoreDto(
            userId,
            score,
            showUps,
            noShows,
            averageRating,
            reportsAgainst,
            showUpRate,
            trustLevel
        );
    }

    private static String determineTrustLevel(int score) {
        if (score >= 150) return "EXCELLENT";
        if (score >= 120) return "GOOD";
        if (score >= 90) return "FAIR";
        if (score >= 60) return "LOW";
        return "POOR";
    }
}
