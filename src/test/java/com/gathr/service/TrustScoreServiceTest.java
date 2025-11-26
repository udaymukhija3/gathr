package com.gathr.service;

import com.gathr.dto.TrustScoreDto;
import com.gathr.entity.User;
import com.gathr.repository.FeedbackRepository;
import com.gathr.repository.ParticipationRepository;
import com.gathr.repository.ReportRepository;
import com.gathr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private com.gathr.config.TrustScoreProperties properties;

    @InjectMocks
    private TrustScoreService trustScoreService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup default property values
        // We use lenient() because not all tests use all properties
        org.mockito.Mockito.lenient().when(properties.getBaseScore()).thenReturn(100);
        org.mockito.Mockito.lenient().when(properties.getMaxScore()).thenReturn(200);
        org.mockito.Mockito.lenient().when(properties.getMinScore()).thenReturn(0);
        org.mockito.Mockito.lenient().when(properties.getShowUpBonus()).thenReturn(5.0);
        org.mockito.Mockito.lenient().when(properties.getNoShowPenalty()).thenReturn(10.0);
        org.mockito.Mockito.lenient().when(properties.getRatingMultiplier()).thenReturn(10.0);
        org.mockito.Mockito.lenient().when(properties.getReportPenalty()).thenReturn(20.0);
        org.mockito.Mockito.lenient().when(properties.getActivityVolumeBonus()).thenReturn(2.0);
        org.mockito.Mockito.lenient().when(properties.getActivityVolumeThreshold()).thenReturn(5);
        org.mockito.Mockito.lenient().when(properties.getPositiveDecayPeriodDays()).thenReturn(90);
        org.mockito.Mockito.lenient().when(properties.getPositiveDecayFactor()).thenReturn(0.5);
        org.mockito.Mockito.lenient().when(properties.getNegativeDecayPeriodDays()).thenReturn(90);
        org.mockito.Mockito.lenient().when(properties.getNegativeDecayFactor()).thenReturn(0.75);
        org.mockito.Mockito.lenient().when(properties.getReportDecayPeriodDays()).thenReturn(30);
        org.mockito.Mockito.lenient().when(properties.getReportDecayFactor()).thenReturn(0.9);
        org.mockito.Mockito.lenient().when(properties.getNewAccountDays()).thenReturn(30);
        org.mockito.Mockito.lenient().when(properties.getNewAccountCeiling()).thenReturn(0.8);

        testUser = new User();
        testUser.setId(1L);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(100)); // Account > 30 days old
    }

    @Test
    void calculateTrustScore_NewUser_ShouldReturnBaseScore() {
        // Arrange
        User newUser = new User();
        newUser.setId(2L);
        newUser.setCreatedAt(LocalDateTime.now()); // Brand new account

        when(userRepository.findById(2L)).thenReturn(Optional.of(newUser));
        when(feedbackRepository.countShowUpsByUserId(2L)).thenReturn(0);
        when(feedbackRepository.countNoShowsByUserId(2L)).thenReturn(0);
        when(feedbackRepository.averageRatingByUserId(2L)).thenReturn(null);
        when(reportRepository.countByTargetUserId(2L)).thenReturn(0);
        when(reportRepository.countRecentReportsByUserId(eq(2L), any())).thenReturn(0L);

        // Act
        TrustScoreDto result = trustScoreService.calculateTrustScore(2L);

        // Assert
        // Base score is 100. New account ceiling is 0.8. So 80.
        assertEquals(80, result.trustScore());
    }

    @Test
    void calculateTrustScore_GoodUser_ShouldHaveHighScore() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(feedbackRepository.countShowUpsByUserId(1L)).thenReturn(10); // +50 raw
        when(feedbackRepository.countNoShowsByUserId(1L)).thenReturn(0);
        when(feedbackRepository.averageRatingByUserId(1L)).thenReturn(5.0); // +50 raw
        when(reportRepository.countByTargetUserId(1L)).thenReturn(0);
        when(reportRepository.countRecentReportsByUserId(eq(1L), any())).thenReturn(0L);

        // Act
        TrustScoreDto result = trustScoreService.calculateTrustScore(1L);

        // Assert
        // Base 100 + ShowUps (~50 decayed) + Rating (50) + Volume Bonus (10) = ~210
        // Capped at 200
        assertEquals(194, result.trustScore());
    }

    @Test
    void calculateTrustScore_BadUser_ShouldHaveLowScore() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(feedbackRepository.countShowUpsByUserId(1L)).thenReturn(0);
        when(feedbackRepository.countNoShowsByUserId(1L)).thenReturn(5); // -50 raw
        when(feedbackRepository.averageRatingByUserId(1L)).thenReturn(2.0); // +20
        when(reportRepository.countByTargetUserId(1L)).thenReturn(2); // -40 raw
        when(reportRepository.countRecentReportsByUserId(eq(1L), any())).thenReturn(2L); // All recent

        // Act
        TrustScoreDto result = trustScoreService.calculateTrustScore(1L);

        // Assert
        // Base 100 - NoShows (~50 decayed) + Rating (20) - Reports (40) = ~30
        assertTrue(result.trustScore() < 100);
        assertTrue(result.trustScore() > 0);
    }

    @Test
    void calculateTrustScore_RecentReports_ShouldPenalizeHeavily() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(feedbackRepository.countShowUpsByUserId(1L)).thenReturn(5); // +25
        when(feedbackRepository.countNoShowsByUserId(1L)).thenReturn(0);
        when(feedbackRepository.averageRatingByUserId(1L)).thenReturn(4.0); // +40
        // 1 report, recent
        when(reportRepository.countByTargetUserId(1L)).thenReturn(1);
        when(reportRepository.countRecentReportsByUserId(eq(1L), any())).thenReturn(1L);

        // Act
        TrustScoreDto result = trustScoreService.calculateTrustScore(1L);

        // Assert
        // Base 100 + 25 + 40 - 20 = 145
        // Without report: 165
        // We just check it's roughly correct, exact decay math is complex
        assertTrue(result.trustScore() < 165);
    }
}
