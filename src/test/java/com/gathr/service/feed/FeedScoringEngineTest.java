package com.gathr.service.feed;

import com.gathr.dto.ActivityDto;
import com.gathr.dto.ScoredActivityDto;
import com.gathr.dto.TrustScoreDto;
import com.gathr.entity.Activity;
import com.gathr.entity.Activity.ActivityCategory;
import com.gathr.entity.ActivityMetrics;
import com.gathr.entity.Hub;
import com.gathr.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FeedScoringEngineTest {

    private FeedScoringEngine feedScoringEngine;
    private StubMutualCountProvider mutualCountProvider;
    private StubTrustScoreProvider trustScoreProvider;
    private StubActivityMetricsProvider metricsProvider;

    private Activity baseActivity;
    private ActivityDto baseDto;
    private User host;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setId(42L);
        host.setName("Host");

        Hub hub = new Hub();
        hub.setLatitude(BigDecimal.valueOf(28.5));
        hub.setLongitude(BigDecimal.valueOf(77.1));

        baseActivity = new Activity();
        baseActivity.setId(1L);
        baseActivity.setTitle("Sunset Run");
        baseActivity.setCategory(ActivityCategory.SPORTS);
        baseActivity.setStartTime(LocalDateTime.now().plusHours(2));
        baseActivity.setEndTime(LocalDateTime.now().plusHours(3));
        baseActivity.setLatitude(28.51);
        baseActivity.setLongitude(77.11);
        baseActivity.setHub(hub);
        baseActivity.setCreatedBy(host);
        baseActivity.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        baseDto = buildDto(baseActivity, 1, 1, 8);

        mutualCountProvider = new StubMutualCountProvider();
        trustScoreProvider = new StubTrustScoreProvider(new TrustScoreDto(host.getId(), 140, 0, 0, 4.5, 0, 100.0, "GOOD"));
        metricsProvider = new StubActivityMetricsProvider(Optional.empty());
        feedScoringEngine = new FeedScoringEngine(mutualCountProvider, trustScoreProvider, metricsProvider);
    }

    @Test
    void score_ShouldReturnEmpty_WhenNoSpotsRemain() {
        FeedScoringContext context = FeedScoringContext.builder()
                .userId(99L)
                .activity(baseActivity)
                .activityDto(baseDto)
                .userInterests(List.of(baseActivity.getCategory().name()))
                .now(LocalDateTime.now())
                .coldStartType(ColdStartType.NONE)
                .locationContext(new LocationContext(28.4, 77.0))
                .preferredHour(19)
                .successCounts(Map.of())
                .spotsRemaining(0)
                .build();

        assertThat(feedScoringEngine.score(context)).isEmpty();
    }

    @Test
    void score_ShouldPrioritizeInterestMatch() {
        FeedScoringContext context = FeedScoringContext.builder()
                .userId(99L)
                .activity(baseActivity)
                .activityDto(baseDto)
                .userInterests(List.of(baseActivity.getCategory().name()))
                .now(LocalDateTime.now())
                .coldStartType(ColdStartType.NEW_USER_WITH_INTERESTS)
                .locationContext(new LocationContext(null, null))
                .preferredHour(19)
                .successCounts(Map.of())
                .spotsRemaining(5)
                .build();

        Optional<ScoredActivityDto> scored = feedScoringEngine.score(context);

        assertThat(scored).isPresent();
        assertThat(scored.get().getScore()).isGreaterThan(0.3);
        assertThat(scored.get().getPrimaryReason()).contains("Matches your");
        assertThat(scored.get().getMutualCount()).isNull();
    }

    @Test
    void score_ShouldIncludeMutualsAndDistanceMetadata() {
        mutualCountProvider.setMutualCount(2);

        FeedScoringContext context = FeedScoringContext.builder()
                .userId(200L)
                .activity(baseActivity)
                .activityDto(baseDto)
                .userInterests(List.of()) // ensures mutuals reason becomes primary
                .now(LocalDateTime.now())
                .coldStartType(ColdStartType.NONE)
                .locationContext(new LocationContext(28.50, 77.13))
                .preferredHour(19)
                .successCounts(Map.of())
                .spotsRemaining(3)
                .build();

        Optional<ScoredActivityDto> scored = feedScoringEngine.score(context);

        assertThat(scored).isPresent();
        ScoredActivityDto dto = scored.get();
        assertThat(dto.getActivity().getMutualsCount()).isEqualTo(2);
        assertThat(dto.getMetadata()).containsKeys("distanceKm", "mutualCount");
        assertThat(dto.getPrimaryReason()).contains("2 friends");
    }

    private static class StubMutualCountProvider implements MutualCountProvider {
        private int mutualCount;

        void setMutualCount(int mutualCount) {
            this.mutualCount = mutualCount;
        }

        @Override
        public int getMutualCount(Long userId, Long activityId) {
            return mutualCount;
        }
    }

    private static class StubTrustScoreProvider implements TrustScoreProvider {
        private TrustScoreDto trustScoreDto;

        StubTrustScoreProvider(TrustScoreDto trustScoreDto) {
            this.trustScoreDto = trustScoreDto;
        }

        void setTrustScore(TrustScoreDto trustScoreDto) {
            this.trustScoreDto = trustScoreDto;
        }

        @Override
        public TrustScoreDto calculate(Long userId) {
            return trustScoreDto;
        }
    }

    private static class StubActivityMetricsProvider implements ActivityMetricsProvider {
        private Optional<ActivityMetrics> metrics = Optional.empty();

        StubActivityMetricsProvider(Optional<ActivityMetrics> metrics) {
            this.metrics = metrics;
        }

        void setMetrics(Optional<ActivityMetrics> metrics) {
            this.metrics = metrics;
        }

        @Override
        public Optional<ActivityMetrics> findById(Long activityId) {
            return metrics;
        }
    }

    private ActivityDto buildDto(Activity activity, int confirmed, int interested, int maxMembers) {
        ActivityDto dto = new ActivityDto();
        dto.setId(activity.getId());
        dto.setTitle(activity.getTitle());
        dto.setHubId(activity.getHub() != null ? activity.getHub().getId() : null);
        dto.setHubName(activity.getHub() != null ? activity.getHub().getName() : null);
        dto.setCategory(activity.getCategory());
        dto.setStartTime(activity.getStartTime());
        dto.setEndTime(activity.getEndTime());
        dto.setCreatedBy(host.getId());
        dto.setCreatedByName(host.getName());
        dto.setMaxMembers(maxMembers);
        dto.setConfirmedCount(confirmed);
        dto.setInterestedCount(interested);
        dto.setTotalParticipants(confirmed + interested);
        dto.setPeopleCount(confirmed + interested);
        dto.setIsInviteOnly(false);
        return dto;
    }
}

