package com.gathr.controller;

import com.gathr.dto.FeedResponse;
import com.gathr.dto.FeedInteractionRequest;
import com.gathr.dto.ScoredActivityDto;
import com.gathr.entity.FeedMetric;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.FeedMetricRepository;
import com.gathr.repository.UserRepository;
import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.EventLogService;
import com.gathr.service.FeedComputationResult;
import com.gathr.service.FeedService;
import com.gathr.service.SocialGraphService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
@Slf4j
public class FeedController {

    private final FeedService feedService;
    private final AuthenticatedUserService authenticatedUserService;
    private final EventLogService eventLogService;
    private final UserRepository userRepository;
    private final SocialGraphService socialGraphService;
    private final FeedMetricRepository feedMetricRepository;

    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @RequestParam(required = false) Long hubId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(50) Integer limit,
            Authentication authentication
    ) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        FeedComputationResult result = feedService.getFeedForUser(userId, hubId, date, limit);
        List<ScoredActivityDto> activities = result.getActivities();
        Long resolvedHubId = hubId;
        if (resolvedHubId == null && !activities.isEmpty()) {
            resolvedHubId = activities.get(0).getActivity().getHubId();
        }

        LocalDate responseDate = date != null ? date : LocalDate.now();
        Map<String, Object> props = new HashMap<>();
        props.put("requestedHubId", hubId);
        props.put("resolvedHubId", resolvedHubId);
        props.put("date", responseDate.toString());
        props.put("limit", limit);
        props.put("resultCount", activities.size());
        eventLogService.log(userId, "feed_requested", props);

        FeedResponse response = FeedResponse.builder()
                .activities(activities)
                .hubId(resolvedHubId)
                .date(responseDate)
                .totalCount(activities.size())
                .userId(userId)
                .fallbackUsed(result.isFallbackUsed())
                .suggestions(result.getSuggestions())
                .meta(result.getFeedMeta())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/feedback")
    public ResponseEntity<Void> trackFeedback(
            @Valid @RequestBody FeedInteractionRequest request,
            Authentication authentication
    ) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("action", request.getAction());
        eventProps.put("position", request.getPosition());
        eventProps.put("score", request.getScore());
        eventProps.put("sessionId", request.getSessionId());
        eventProps.put("hubId", request.getHubId());
        eventLogService.log(userId, request.getActivityId(), "feed_interaction", eventProps);

        // Persist structured metrics for analytics / ML training
        FeedMetric metric = new FeedMetric();
        metric.setUserId(userId);
        metric.setActivityId(request.getActivityId());
        metric.setHubId(request.getHubId());
        metric.setScore(request.getScore());
        metric.setPosition(request.getPosition());
        metric.setAction(request.getAction());
        metric.setSessionId(request.getSessionId());
        metric.setActionTimestamp(LocalDateTime.now());
        feedMetricRepository.save(metric);

        switch (request.getAction()) {
            case "clicked" -> eventLogService.log(userId, request.getActivityId(),
                    "activity_clicked_from_feed", Map.of("source", "feed"));
            case "joined" -> eventLogService.log(userId, request.getActivityId(),
                    "activity_joined_from_feed", Map.of("source", "feed"));
            case "dismissed" -> eventLogService.log(userId, request.getActivityId(),
                    "activity_dismissed_from_feed", Map.of("source", "feed"));
            default -> {
            }
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshFeed(Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        feedService.invalidateUserFeedCache(userId);
        eventLogService.log(userId, "feed_cache_cleared", Map.of());

        return ResponseEntity.ok(Map.of(
                "message", "Feed cache cleared",
                "userId", userId.toString()
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFeedStats(Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        long mutualConnections = socialGraphService.countApproximateMutualConnections(userId);
        int interestsCount = user.getInterests() != null ? user.getInterests().length : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("personalizationEnabled", user.hasCompletedOnboarding());
        stats.put("interestsCount", interestsCount);
        stats.put("mutualConnectionsCount", mutualConnections);
        stats.put("homeHubId", user.getHomeHub() != null ? user.getHomeHub().getId() : null);
        stats.put("preferredRadiusKm", user.getPreferredRadiusKm());
        stats.put("lastActive", user.getLastActive());
        stats.put("cacheEnabled", true);

        return ResponseEntity.ok(stats);
    }
}