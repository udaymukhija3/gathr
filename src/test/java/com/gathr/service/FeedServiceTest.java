package com.gathr.service;

import com.gathr.dto.ActivityDto;
import com.gathr.dto.ScoredActivityDto;
import com.gathr.entity.Activity;
import com.gathr.entity.Hub;
import com.gathr.entity.User;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.ParticipationRepository;
import com.gathr.repository.UserRepository;
import com.gathr.service.FeedComputationResult;
import com.gathr.service.feed.FeedScoringContext;
import com.gathr.service.feed.FeedScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityService activityService;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private FeedScoringEngine feedScoringEngine;
    @Mock
    private com.gathr.config.TrustScoreProperties properties;
    @Mock
    private EventLogService eventLogService;

    @InjectMocks
    private FeedService feedService;

    private User testUser;
    private Hub testHub;
    private Activity testActivity;
    private ActivityDto testActivityDto;

    @BeforeEach
    void setUp() {
        testHub = new Hub();
        testHub.setId(1L);
        testHub.setName("Test Hub");

        testUser = new User();
        testUser.setId(1L);
        testUser.setHomeHub(testHub);
        testUser.setInterests(new String[] { "SPORTS" });

        testActivity = new Activity();
        testActivity.setId(100L);
        testActivity.setTitle("Soccer Game");
        testActivity.setCategory(Activity.ActivityCategory.SPORTS);
        testActivity.setHub(testHub);
        testActivity.setMaxMembers(10);

        testActivityDto = new ActivityDto();
        testActivityDto.setId(100L);
        testActivityDto.setTitle("Soccer Game");
        testActivityDto.setCategory(Activity.ActivityCategory.SPORTS);
        testActivityDto.setMaxMembers(10);
        testActivityDto.setConfirmedCount(2);
        testActivityDto.setInterestedCount(1);
    }

    @Test
    void getFeedForUser_PersonalizedFeed_ShouldReturnActivities() {
        // Arrange
        LocalDate today = LocalDate.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.findAll(any(Specification.class))).thenReturn(List.of(testActivity));
        when(activityService.convertToDto(testActivity, true)).thenReturn(testActivityDto);

        ScoredActivityDto scoredDto = ScoredActivityDto.builder()
                .activity(testActivityDto)
                .score(0.9)
                .build();
        when(feedScoringEngine.score(any(FeedScoringContext.class))).thenReturn(Optional.of(scoredDto));

        // Act
        FeedComputationResult result = feedService.getFeedForUser(1L, 1L, today, 10);

        // Assert
        assertFalse(result.isFallbackUsed());
        assertEquals(1, result.getActivities().size());
        assertEquals(100L, result.getActivities().get(0).getActivity().getId());
    }

    @Test
    void getFeedForUser_NoActivities_ShouldUseFallback() {
        // Arrange
        LocalDate today = LocalDate.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // Empty list for primary search
        when(activityRepository.findAll(any(Specification.class)))
                .thenReturn(Collections.emptyList()) // Primary search empty
                .thenReturn(List.of(testActivity)); // Fallback search returns activity
        when(activityService.convertToDto(testActivity, true)).thenReturn(testActivityDto);

        // Act
        FeedComputationResult result = feedService.getFeedForUser(1L, 1L, today, 10);

        // Assert
        assertTrue(result.isFallbackUsed());
        assertEquals(1, result.getActivities().size());
        assertEquals(0.35, result.getActivities().get(0).getScore()); // Fallback score
    }

    @Test
    void getFeedForUser_FullActivity_ShouldBeFilteredOrFlagged() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Activity is full
        testActivityDto.setConfirmedCount(10);
        testActivityDto.setInterestedCount(0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.findAll(any(Specification.class))).thenReturn(List.of(testActivity));
        // participationRepository stub removed
        when(activityService.convertToDto(testActivity, true)).thenReturn(testActivityDto);

        // Act
        FeedComputationResult result = feedService.getFeedForUser(1L, 1L, today, 10);

        // Assert
        // Should be filtered out by computeSpotsRemaining check in
        // computePersonalizedFeed
        // Consequently, it triggers fallback logic
        assertTrue(result.isFallbackUsed());
        // Fallback returns the activity but with lower score
        assertFalse(result.getActivities().isEmpty());
        assertEquals(0.2, result.getActivities().get(0).getScore(), 0.01);
    }
}
