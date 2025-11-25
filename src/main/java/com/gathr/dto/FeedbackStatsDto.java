package com.gathr.dto;

/**
 * DTO for feedback statistics/aggregations.
 */
public record FeedbackStatsDto(
    Long activityId,
    int totalFeedbacks,
    int showedUpCount,
    int noShowCount,
    Double averageRating,
    int wouldHangOutAgainCount,
    int addedToContactsCount,
    double showUpRate
) {
    /**
     * Calculate show-up rate as percentage.
     */
    public static FeedbackStatsDto create(
            Long activityId,
            int totalFeedbacks,
            int showedUpCount,
            Double averageRating,
            int wouldHangOutAgainCount,
            int addedToContactsCount
    ) {
        int noShowCount = totalFeedbacks - showedUpCount;
        double showUpRate = totalFeedbacks > 0
            ? (double) showedUpCount / totalFeedbacks * 100
            : 0.0;

        return new FeedbackStatsDto(
            activityId,
            totalFeedbacks,
            showedUpCount,
            noShowCount,
            averageRating,
            wouldHangOutAgainCount,
            addedToContactsCount,
            showUpRate
        );
    }
}
