package com.gathr.service.feed;

import com.gathr.dto.ActivityDto;
import com.gathr.dto.ScoredActivityDto;
import com.gathr.dto.TrustScoreDto;
import com.gathr.entity.Activity;
import com.gathr.entity.ActivityMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the scoring heuristics for personalized feeds so the logic
 * can be unit tested independently of {@link com.gathr.service.FeedService}.
 */
@Component
@RequiredArgsConstructor
public class FeedScoringEngine {

    private static final double INTEREST_WEIGHT = 0.35;
    private static final double MUTUALS_WEIGHT = 0.25;
    private static final double FRESHNESS_WEIGHT = 0.15;
    private static final double AVAILABILITY_WEIGHT = 0.15;
    private static final double TRUST_WEIGHT = 0.05;
    private static final double POPULARITY_WEIGHT = 0.05;

    private final MutualCountProvider mutualCountProvider;
    private final TrustScoreProvider trustScoreProvider;
    private final ActivityMetricsProvider activityMetricsProvider;

    public Optional<ScoredActivityDto> score(FeedScoringContext ctx) {
        Activity activity = ctx.activity();
        ActivityDto dto = ctx.activityDto();
        int spotsRemaining = ctx.spotsRemaining();

        if (spotsRemaining == 0) {
            return Optional.empty();
        }

        double score = 0.0;
        List<String> reasons = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("coldStartType", ctx.coldStartType().name());

        score += scoreInterestMatch(ctx, activity, reasons);
        score += scoreMutuals(ctx, activity, reasons, metadata);
        score += scoreFreshness(ctx.now(), activity, reasons, metadata);

        score += scoreAvailability(dto, spotsRemaining, reasons, metadata);
        score += scoreCreatorTrust(activity, metadata, reasons);
        score += scorePopularity(activity, metadata, reasons);
        score += scoreRecencyBonus(ctx.now(), activity, metadata, reasons);
        score += scoreDistance(ctx.locationContext(), activity, reasons, metadata);
        score += scoreTimePreference(ctx, activity, metadata);
        score += scoreCategorySuccess(ctx, activity, metadata, reasons);
        score += adjustForColdStart(ctx, reasons);

        if (score <= 0) {
            return Optional.empty();
        }

        Integer mutualCount = dto.getMutualsCount();
        String primaryReason = !reasons.isEmpty() ? reasons.get(0) : "Recommended for you";
        String secondaryReason = reasons.size() > 1 ? reasons.get(1) : null;
        double roundedScore = Math.round(score * 1000d) / 1000d;

        return Optional.of(
                ScoredActivityDto.builder()
                        .activity(dto)
                        .score(roundedScore)
                        .primaryReason(primaryReason)
                        .secondaryReason(secondaryReason)
                        .mutualCount(mutualCount)
                        .spotsRemaining(spotsRemaining >= 0 ? spotsRemaining : null)
                        .metadata(metadata)
                        .diversityPenaltyApplied(false)
                        .build()
        );
    }

    private double scoreInterestMatch(
            FeedScoringContext ctx,
            Activity activity,
            List<String> reasons
    ) {
        List<String> interests = ctx.userInterests();
        if (interests == null || interests.isEmpty()) {
            return 0.05;
        }

        if (activity.getCategory() == null) {
            return 0.0;
        }

        String categoryName = activity.getCategory().name();
        if (interests.contains(categoryName)) {
            reasons.add("Matches your " + formatCategory(categoryName) + " interest");
            if (ctx.coldStartType() == ColdStartType.NEW_USER_WITH_INTERESTS) {
                return INTEREST_WEIGHT + 0.1;
            }
            return INTEREST_WEIGHT;
        }
        return 0.0;
    }

    private double scoreMutuals(
            FeedScoringContext ctx,
            Activity activity,
            List<String> reasons,
            Map<String, Object> metadata
    ) {
        int mutualCount = mutualCountProvider.getMutualCount(ctx.userId(), activity.getId());
        if (mutualCount <= 0) {
            ctx.activityDto().setMutualsCount(null);
            return 0.0;
        }

        ctx.activityDto().setMutualsCount(mutualCount);
        double normalized = Math.min(1.0, mutualCount / 3.0);
        metadata.put("mutualCount", mutualCount);

        if (ctx.coldStartType() == ColdStartType.INACTIVE_USER_RETURNING) {
            normalized += 0.2;
        }

        reasons.add(mutualCount + (mutualCount == 1 ? " friend is going" : " friends are going"));
        return MUTUALS_WEIGHT * normalized;
    }

    private double scoreFreshness(
            LocalDateTime now,
            Activity activity,
            List<String> reasons,
            Map<String, Object> metadata
    ) {
        if (activity.getStartTime() == null) {
            return 0.0;
        }

        long hoursUntilStart = ChronoUnit.HOURS.between(now, activity.getStartTime());
        if (hoursUntilStart < 0 || hoursUntilStart > 12) {
            return 0.0;
        }

        double freshnessScore = 1.0 - (hoursUntilStart / 12.0);
        metadata.put("hoursUntilStart", hoursUntilStart);

        if (hoursUntilStart <= 1) {
            reasons.add("Starting within the hour");
        } else if (hoursUntilStart <= 6) {
            reasons.add("Starting soon");
        }

        return FRESHNESS_WEIGHT * freshnessScore;
    }

    private double scoreAvailability(
            ActivityDto dto,
            int spotsRemaining,
            List<String> reasons,
            Map<String, Object> metadata
    ) {
        if (spotsRemaining <= 0 || dto.getMaxMembers() == null || dto.getMaxMembers() <= 0) {
            return 0.0;
        }

        double totalParticipants = dto.getTotalParticipants() != null ? dto.getTotalParticipants() : 0;
        double fillRatio = totalParticipants / dto.getMaxMembers();
        double availabilityScore;
        if (fillRatio >= 0.9) {
            availabilityScore = 0.9;
        } else if (fillRatio >= 0.5) {
            availabilityScore = 1.0;
        } else if (fillRatio == 0.0) {
            availabilityScore = 0.4;
        } else {
            availabilityScore = 0.7;
        }

        if (spotsRemaining <= 3) {
            reasons.add("Only " + spotsRemaining + " spot" + (spotsRemaining > 1 ? "s" : "") + " left");
        }
        metadata.put("spotsRemaining", spotsRemaining);
        return AVAILABILITY_WEIGHT * availabilityScore;
    }

    private double scoreCreatorTrust(Activity activity, Map<String, Object> metadata, List<String> reasons) {
        if (activity.getCreatedBy() == null) {
            return 0.0;
        }
        TrustScoreDto trustScore = trustScoreProvider.calculate(activity.getCreatedBy().getId());
        double normalizedTrust = Math.min(1.0, Math.max(0.0, trustScore.trustScore() / 200.0));
        metadata.put("creatorTrustScore", trustScore.trustScore());
        if (normalizedTrust >= 0.6) {
            metadata.put("trustedHost", true);
            reasons.add("Hosted by a trusted member");
        }
        return TRUST_WEIGHT * normalizedTrust;
    }

    private double scorePopularity(Activity activity, Map<String, Object> metadata, List<String> reasons) {
        Optional<ActivityMetrics> metricsOpt = activityMetricsProvider.findById(activity.getId());
        if (metricsOpt.isEmpty()) {
            return 0.0;
        }
        ActivityMetrics metrics = metricsOpt.get();
        if (metrics.getTotalJoins() == null) {
            return 0.0;
        }
        double popularityScore = Math.min(1.0, metrics.getTotalJoins() / 15.0);
        metadata.put("totalJoins", metrics.getTotalJoins());
        if (metrics.getTotalJoins() > 5) {
            reasons.add("Popular plan");
        }
        return POPULARITY_WEIGHT * popularityScore;
    }

    private double scoreRecencyBonus(
            LocalDateTime now,
            Activity activity,
            Map<String, Object> metadata,
            List<String> reasons
    ) {
        if (activity.getCreatedAt() == null) {
            return 0.0;
        }
        if (activity.getCreatedAt().isAfter(now.minusHours(2))) {
            metadata.put("newActivityBoost", true);
            reasons.add("New activity – be the first to join");
            return 0.08;
        }
        return 0.0;
    }

    private double scoreDistance(
            LocationContext locationContext,
            Activity activity,
            List<String> reasons,
            Map<String, Object> metadata
    ) {
        if (locationContext == null) {
            return 0.0;
        }
        Double userLat = locationContext.latitude();
        Double userLon = locationContext.longitude();
        Double activityLat = resolveLatitude(activity);
        Double activityLon = resolveLongitude(activity);
        if (userLat == null || userLon == null || activityLat == null || activityLon == null) {
            return 0.0;
        }

        double distanceKm = calculateDistanceKm(userLat, userLon, activityLat, activityLon);
        metadata.put("distanceKm", Math.round(distanceKm * 10d) / 10d);

        if (distanceKm <= 2) {
            reasons.add("Near you");
            return 0.12;
        } else if (distanceKm <= 5) {
            reasons.add("Quick to reach");
            return 0.08;
        } else if (distanceKm > 10) {
            return -0.04;
        }
        return 0.0;
    }

    private double scoreTimePreference(
            FeedScoringContext ctx,
            Activity activity,
            Map<String, Object> metadata
    ) {
        if (ctx.preferredHour() < 0 || activity.getStartTime() == null) {
            return 0.0;
        }
        int activityHour = activity.getStartTime().getHour();
        double timeAlignment = Math.exp(-Math.pow(ctx.preferredHour() - activityHour, 2) / 18.0);
        metadata.put("timeAlignment", timeAlignment);
        if (timeAlignment > 0.7) {
            metadata.put("scheduleMatch", true);
        }
        return 0.1 * timeAlignment;
    }

    private double scoreCategorySuccess(
            FeedScoringContext ctx,
            Activity activity,
            Map<String, Object> metadata,
            List<String> reasons
    ) {
        if (activity.getCategory() == null || ctx.successCounts() == null) {
            return 0.0;
        }
        Long categorySuccess = ctx.successCounts().get(activity.getCategory());
        if (categorySuccess == null || categorySuccess == 0) {
            return 0.0;
        }
        double affinity = Math.min(1.0, categorySuccess / 5.0);
        metadata.put("successCount", categorySuccess);
        reasons.add("You've enjoyed similar plans");
        return 0.08 * affinity;
    }

    private double adjustForColdStart(FeedScoringContext ctx, List<String> reasons) {
        return switch (ctx.coldStartType()) {
            case NEW_USER_NO_INTERESTS -> {
                if (!reasons.contains("Popular in this hub")) {
                    reasons.add("Popular in this hub");
                }
                yield 0.12;
            }
            case RETURNING_USER_NEW_HUB -> 0.05;
            case INACTIVE_USER_RETURNING -> {
                if (!reasons.contains("Welcome back!")) {
                    reasons.add("Welcome back!");
                }
                yield 0.05;
            }
            default -> 0.0;
        };
    }

    private String formatCategory(String category) {
        if (category == null || category.isBlank()) {
            return "activity";
        }
        String lower = category.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private Double resolveLatitude(Activity activity) {
        if (activity.getLatitude() != null) {
            return activity.getLatitude();
        }
        if (activity.getHub() != null && activity.getHub().getLatitude() != null) {
            return activity.getHub().getLatitude().doubleValue();
        }
        return null;
    }

    private Double resolveLongitude(Activity activity) {
        if (activity.getLongitude() != null) {
            return activity.getLongitude();
        }
        if (activity.getHub() != null && activity.getHub().getLongitude() != null) {
            return activity.getHub().getLongitude().doubleValue();
        }
        return null;
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(rLat1) * Math.cos(rLat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}

