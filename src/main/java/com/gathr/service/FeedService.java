package com.gathr.service;

import com.gathr.dto.ActivityDto;
import com.gathr.dto.ScoredActivityDto;
import com.gathr.entity.Activity;
import com.gathr.entity.Activity.ActivityCategory;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.ParticipationRepository;
import com.gathr.repository.UserRepository;
import com.gathr.service.feed.ColdStartType;
import com.gathr.service.feed.FeedScoringContext;
import com.gathr.service.feed.FeedScoringEngine;
import com.gathr.service.feed.LocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FeedService {

    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final ParticipationRepository participationRepository;
    private final FeedScoringEngine feedScoringEngine;
    private final com.gathr.config.TrustScoreProperties properties;
    private final EventLogService eventLogService;

    public FeedService(
            ActivityRepository activityRepository,
            UserRepository userRepository,
            ActivityService activityService,
            ParticipationRepository participationRepository,
            FeedScoringEngine feedScoringEngine,
            com.gathr.config.TrustScoreProperties properties,
            EventLogService eventLogService) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.participationRepository = participationRepository;
        this.feedScoringEngine = feedScoringEngine;
        this.properties = properties;
        this.eventLogService = eventLogService;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "feed", key = "T(String).format('%s:%s:%s:%s', #userId, #hubId == null ? 'default' : #hubId, "
            +
            "(#date == null ? T(java.time.LocalDate).now() : #date).toString(), #limit)", condition = "#limit <= 50")
    public FeedComputationResult getFeedForUser(Long userId, Long hubId, LocalDate date, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Long resolvedHubId = resolveHubId(user, hubId);
        if (resolvedHubId == null) {
            logger.warn("No hub resolved for user {}. Returning empty feed.", userId);
            return FeedComputationResult.builder()
                    .activities(List.of())
                    .fallbackUsed(true)
                    .suggestions(List.of("Set a home hub to unlock personalized recommendations"))
                    .build();
        }

        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<ScoredActivityDto> activities;
        boolean fallbackUsed = false;
        List<String> suggestions = new ArrayList<>();
        LocationContext locationContext = resolveLocationContext(user);
        Map<ActivityCategory, Long> successCounts = getCategorySuccessCounts(userId);
        int preferredHour = resolvePreferredStartHour(user, userId);

        try {
            activities = computePersonalizedFeed(
                    user,
                    userId,
                    resolvedHubId,
                    targetDate,
                    limit,
                    locationContext,
                    preferredHour,
                    successCounts);
        } catch (Exception ex) {
            logger.error("Primary recommendation computation failed for user {}", userId, ex);
            activities = buildFallbackFeed(resolvedHubId, targetDate, limit);
            fallbackUsed = true;
        }

        if (activities.isEmpty()) {
            fallbackUsed = true;
            suggestions.add("No activities found for the selected date.");
            suggestions.add("Try a different date or expand to nearby hubs.");
            activities = buildFallbackFeed(resolvedHubId, targetDate.plusDays(1), limit);
        }

        if (!activities.isEmpty() && activities.stream().allMatch(this::isActivityFull)) {
            suggestions.add("All current activities are full. Join the waitlist or check tomorrow.");
        }

        return FeedComputationResult.builder()
                .activities(activities)
                .fallbackUsed(fallbackUsed)
                .suggestions(suggestions)
                .feedMeta(buildFeedMeta(activities, resolvedHubId, targetDate, fallbackUsed))
                .build();
    }

    private List<ScoredActivityDto> computePersonalizedFeed(
            User user,
            Long userId,
            Long hubId,
            LocalDate targetDate,
            int limit,
            LocationContext locationContext,
            int preferredHour,
            Map<ActivityCategory, Long> successCounts) {
        // Use Specification to filter at DB level
        List<Activity> activities = activityRepository.findAll(
                com.gathr.repository.spec.ActivitySpecification.withFilters(hubId, targetDate, userId));

        if (activities.isEmpty()) {
            logger.info("No activities found for hub {} on {}", hubId, targetDate);
            return List.of();
        }

        List<String> interestList = getInterestList(user);
        LocalDateTime now = LocalDateTime.now();
        ColdStartType coldStartType = detectColdStartType(user, hubId);

        // No need to fetch participated IDs separately as they are excluded in the
        // query
        // But we might need them for other logic?
        // The original code used them to filter. The specification now handles
        // exclusion.

        List<ScoredActivityDto> scoredActivities = activities.stream()
                // .filter(activity -> !participatedActivityIds.contains(activity.getId())) //
                // Handled by DB
                .map(activity -> {
                    ActivityDto dto = activityService.convertToDto(activity, true);
                    int spotsRemaining = computeSpotsRemaining(dto);
                    if (spotsRemaining == 0) {
                        return Optional.<ScoredActivityDto>empty();
                    }
                    FeedScoringContext context = FeedScoringContext.builder()
                            .userId(userId)
                            .activity(activity)
                            .activityDto(dto)
                            .userInterests(interestList)
                            .now(now)
                            .coldStartType(coldStartType)
                            .locationContext(locationContext)
                            .preferredHour(preferredHour)
                            .successCounts(successCounts)
                            .spotsRemaining(spotsRemaining)
                            .build();
                    return feedScoringEngine.score(context);
                })
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(ScoredActivityDto::getScore).reversed())
                .collect(Collectors.toList());

        if (scoredActivities.size() > 10) {
            scoredActivities = applyDiversity(scoredActivities);
        }

        int effectiveLimit = limit > 0 ? limit : 20;
        scoredActivities = applyExplorationBlend(scoredActivities, interestList, effectiveLimit);

        return scoredActivities.stream()
                .limit(effectiveLimit)
                .collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = "feed", allEntries = true)
    public void invalidateUserFeedCache(Long userId) {
        logger.info("Invalidating feed cache for user {}", userId);
    }

    private List<ScoredActivityDto> applyDiversity(List<ScoredActivityDto> scoredActivities) {
        Map<String, Integer> categoryCounts = new HashMap<>();
        List<ScoredActivityDto> reordered = new ArrayList<>();

        for (ScoredActivityDto scored : scoredActivities) {
            ActivityDto activity = scored.getActivity();
            if (activity.getCategory() == null) {
                reordered.add(scored);
                continue;
            }

            String category = activity.getCategory().name();
            int count = categoryCounts.getOrDefault(category, 0);

            if (count >= 2) {
                double penaltyFactor = Math.max(0.5, 1.0 - (count - 1) * 0.2);
                scored.setScore(Math.round(scored.getScore() * penaltyFactor * 1000d) / 1000d);
                scored.setDiversityPenaltyApplied(true);
            }

            reordered.add(scored);
            categoryCounts.put(category, count + 1);
        }

        reordered.sort(Comparator.comparing(ScoredActivityDto::getScore).reversed());
        return reordered;
    }

    private List<ScoredActivityDto> applyExplorationBlend(
            List<ScoredActivityDto> scoredActivities,
            List<String> userInterests,
            int limit) {
        if (scoredActivities.isEmpty() || userInterests == null || userInterests.size() != 1) {
            return scoredActivities;
        }

        String dominantInterest = userInterests.get(0);
        List<ScoredActivityDto> interestMatches = new ArrayList<>();
        List<ScoredActivityDto> explorationCandidates = new ArrayList<>();

        for (ScoredActivityDto dto : scoredActivities) {
            String category = dto.getActivity().getCategory() != null
                    ? dto.getActivity().getCategory().name()
                    : null;
            if (dominantInterest.equals(category)) {
                interestMatches.add(dto);
            } else {
                explorationCandidates.add(dto);
            }
        }

        if (explorationCandidates.isEmpty()) {
            return scoredActivities;
        }

        int interestQuota = Math.max(1, Math.round(limit * 0.7f));
        int explorationQuota = Math.max(1, limit - interestQuota);
        List<ScoredActivityDto> blended = new ArrayList<>();
        Set<ScoredActivityDto> seen = new HashSet<>();

        addUpTo(blended, interestMatches, interestQuota, seen);
        addUpTo(blended, explorationCandidates, explorationQuota, seen);

        for (ScoredActivityDto dto : scoredActivities) {
            if (blended.size() >= scoredActivities.size()) {
                break;
            }
            if (seen.add(dto)) {
                blended.add(dto);
            }
        }

        return blended;
    }

    private void addUpTo(
            List<ScoredActivityDto> target,
            List<ScoredActivityDto> source,
            int quota,
            Set<ScoredActivityDto> seen) {
        int added = 0;
        for (ScoredActivityDto dto : source) {
            if (added >= quota) {
                break;
            }
            if (seen.add(dto)) {
                target.add(dto);
                added++;
            }
        }
    }

    private List<String> getInterestList(User user) {
        String[] interests = user.getInterests();
        if (interests == null || interests.length == 0) {
            return List.of();
        }
        return List.of(interests);
    }

    private int computeSpotsRemaining(ActivityDto dto) {
        if (dto.getMaxMembers() == null || dto.getMaxMembers() <= 0) {
            return dto.getMaxMembers() != null ? dto.getMaxMembers() : 0;
        }
        int confirmed = dto.getConfirmedCount() != null ? dto.getConfirmedCount() : 0;
        int interested = dto.getInterestedCount() != null ? dto.getInterestedCount() : 0;
        int spotsLeft = dto.getMaxMembers() - (confirmed + interested);
        return Math.max(spotsLeft, 0);
    }

    private Long resolveHubId(User user, Long hubId) {
        if (hubId != null) {
            return hubId;
        }
        if (user.getHomeHub() != null) {
            return user.getHomeHub().getId();
        }
        // Temporary fallback to Cyberhub
        return 1L;
    }

    private LocationContext resolveLocationContext(User user) {
        Double lat = null;
        Double lon = null;

        if (user.getLatitude() != null && user.getLongitude() != null) {
            lat = user.getLatitude().doubleValue();
            lon = user.getLongitude().doubleValue();
        } else if (user.getHomeLatitude() != null && user.getHomeLongitude() != null) {
            lat = user.getHomeLatitude().doubleValue();
            lon = user.getHomeLongitude().doubleValue();
        }

        return new LocationContext(lat, lon);
    }

    private Map<ActivityCategory, Long> getCategorySuccessCounts(Long userId) {
        List<Object[]> raw = participationRepository.findConfirmedCountByCategory(userId);
        Map<ActivityCategory, Long> counts = new EnumMap<>(ActivityCategory.class);
        for (Object[] row : raw) {
            if (row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            ActivityCategory category = (ActivityCategory) row[0];
            Long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            counts.put(category, count);
        }
        return counts;
    }

    private int resolvePreferredStartHour(User user, Long userId) {
        if (user.getPreferredStartHour() != null) {
            return clampHour(user.getPreferredStartHour());
        }
        Double avgHour = participationRepository.findAverageStartHourForUser(userId);
        if (avgHour != null) {
            return clampHour((int) Math.round(avgHour));
        }
        return 19; // Default to evenings
    }

    private int clampHour(Integer value) {
        if (value == null) {
            return -1;
        }
        return Math.max(0, Math.min(23, value));
    }

    private FeedMeta buildFeedMeta(List<ScoredActivityDto> activities, Long hubId, LocalDate date,
            boolean fallbackUsed) {
        if (activities == null || activities.isEmpty()) {
            return FeedMeta.builder()
                    .ctaText(fallbackUsed ? "Expand your search" : "No activities available")
                    .timeWindowLabel(date != null ? date.toString() : null)
                    .build();
        }

        List<ScoredActivityDto> topFive = activities.stream()
                .limit(5)
                .collect(Collectors.toList());

        boolean happeningSoon = topFive.stream().anyMatch(dto -> {
            if (dto.getActivity().getStartTime() == null) {
                return false;
            }
            long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), dto.getActivity().getStartTime());
            return hours >= 0 && hours <= 3;
        });

        String cta = happeningSoon ? "Happening in the next few hours" : "Plan ahead for upcoming meetups";
        String windowLabel = happeningSoon ? "Next few hours" : "This week";

        Optional<ScoredActivityDto> scarce = topFive.stream()
                .filter(dto -> {
                    int remaining = computeSpotsRemaining(dto.getActivity());
                    return remaining > 0 && remaining <= 2;
                })
                .findFirst();

        String scarcity = scarce
                .map(dto -> {
                    int remaining = computeSpotsRemaining(dto.getActivity());
                    return "Only " + remaining + " spots left in " + dto.getActivity().getTitle();
                })
                .orElse("Plenty of spots available tonight");

        List<Long> topIds = topFive.stream()
                .map(dto -> dto.getActivity().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return FeedMeta.builder()
                .ctaText(cta)
                .timeWindowLabel(windowLabel)
                .scarcityMessage(scarcity)
                .topActivityIds(topIds)
                .build();
    }

    private List<ScoredActivityDto> buildFallbackFeed(Long hubId, LocalDate date, int limit) {
        if (hubId == null) {
            return List.of();
        }

        LocalDate fallbackDate = date != null ? date : LocalDate.now();
        List<ActivityDto> fallbackDtos = activityRepository.findAll(
                com.gathr.repository.spec.ActivitySpecification.withFilters(hubId, fallbackDate, null)).stream()
                .filter(activity -> !Boolean.TRUE.equals(activity.getIsInviteOnly()))
                .map(activity -> activityService.convertToDto(activity, true))
                .collect(Collectors.toList());

        fallbackDtos.sort(Comparator
                .comparing((ActivityDto dto) -> hasAvailability(dto) ? 0 : 1)
                .thenComparing(ActivityDto::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));

        int effectiveLimit = limit > 0 ? limit : 20;

        return fallbackDtos.stream()
                .limit(effectiveLimit)
                .map(dto -> {
                    boolean available = hasAvailability(dto);
                    Map<String, Object> metadata = Map.of(
                            "fallback", true,
                            "available", available);
                    return ScoredActivityDto.builder()
                            .activity(dto)
                            .score(available ? 0.35 : 0.2)
                            .primaryReason(available ? "Happening soon" : "Currently full – join the waitlist")
                            .secondaryReason(
                                    available ? "Spots available right now" : "We'll notify you if a spot opens")
                            .metadata(metadata)
                            .diversityPenaltyApplied(false)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private boolean hasAvailability(ActivityDto dto) {
        if (dto.getMaxMembers() == null || dto.getMaxMembers() == 0) {
            return true;
        }
        int confirmed = dto.getConfirmedCount() != null ? dto.getConfirmedCount() : 0;
        int interested = dto.getInterestedCount() != null ? dto.getInterestedCount() : 0;
        return (confirmed + interested) < dto.getMaxMembers();
    }

    private boolean isActivityFull(ScoredActivityDto scoredActivityDto) {
        return !hasAvailability(scoredActivityDto.getActivity());
    }

    private ColdStartType detectColdStartType(User user, Long hubId) {
        Long userId = user.getId();
        long totalParticipations = participationRepository.countByUserId(userId);
        boolean hasInterests = user.getInterests() != null && user.getInterests().length > 0;

        if (totalParticipations == 0) {
            return hasInterests ? ColdStartType.NEW_USER_WITH_INTERESTS : ColdStartType.NEW_USER_NO_INTERESTS;
        }

        LocalDateTime lastParticipation = participationRepository.findLatestParticipationTs(userId);
        if (lastParticipation != null && lastParticipation.isBefore(LocalDateTime.now().minusDays(30))) {
            return ColdStartType.INACTIVE_USER_RETURNING;
        }

        if (hubId != null) {
            long hubParticipationCount = participationRepository.countByUserIdAndHubId(userId, hubId);
            if (hubParticipationCount == 0) {
                return ColdStartType.RETURNING_USER_NEW_HUB;
            }
        }

        return ColdStartType.NONE;
    }
}