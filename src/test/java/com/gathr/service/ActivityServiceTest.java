package com.gathr.service;

import com.gathr.dto.ActivityDto;
import com.gathr.dto.CreateActivityRequest;
import com.gathr.entity.Activity;
import com.gathr.entity.Hub;
import com.gathr.entity.Participation;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.HubRepository;
import com.gathr.repository.ParticipationRepository;
import com.gathr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private HubRepository hubRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @InjectMocks
    private ActivityService activityService;

    private Hub testHub;
    private User testUser;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        testHub = new Hub();
        testHub.setId(1L);
        testHub.setName("Cyberhub");
        testHub.setArea("Cyber City");
        testHub.setDescription("Test hub");

        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("1234567890");
        testUser.setName("Test User");
        testUser.setVerified(true);

        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setTitle("Test Activity");
        testActivity.setHub(testHub);
        testActivity.setCategory(Activity.ActivityCategory.SPORTS);
        testActivity.setStartTime(LocalDateTime.now().plusHours(2));
        testActivity.setEndTime(LocalDateTime.now().plusHours(4));
        testActivity.setCreatedBy(testUser);
    }

    @Test
    void getActivitiesByHub_ShouldReturnListOfActivityDtos() {
        // Given
        Long hubId = 1L;
        LocalDate today = LocalDate.now();
        List<Activity> activities = Arrays.asList(testActivity);

        when(activityRepository.findByHubIdAndDate(hubId, today)).thenReturn(activities);

        // When
        List<ActivityDto> result = activityService.getActivitiesByHub(hubId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Activity");
        assertThat(result.get(0).getHubName()).isEqualTo("Cyberhub");
        assertThat(result.get(0).getCategory()).isEqualTo(Activity.ActivityCategory.SPORTS);

        verify(activityRepository).findByHubIdAndDate(hubId, today);
    }

    @Test
    void getActivitiesByHub_WithNoActivities_ShouldReturnEmptyList() {
        // Given
        Long hubId = 1L;
        LocalDate today = LocalDate.now();

        when(activityRepository.findByHubIdAndDate(hubId, today)).thenReturn(Arrays.asList());

        // When
        List<ActivityDto> result = activityService.getActivitiesByHub(hubId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(activityRepository).findByHubIdAndDate(hubId, today);
    }

    @Test
    void createActivity_WithValidData_ShouldReturnActivityDto() {
        // Given
        CreateActivityRequest request = new CreateActivityRequest();
        request.setTitle("New Activity");
        request.setHubId(1L);
        request.setCategory(Activity.ActivityCategory.FOOD);
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(4));

        when(hubRepository.findById(1L)).thenReturn(Optional.of(testHub));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        ActivityDto result = activityService.createActivity(request, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Activity");
        assertThat(result.getHubId()).isEqualTo(1L);
        assertThat(result.getCreatedByName()).isEqualTo("Test User");

        verify(hubRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    void createActivity_WithInvalidHub_ShouldThrowException() {
        // Given
        CreateActivityRequest request = new CreateActivityRequest();
        request.setHubId(999L);

        when(hubRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> activityService.createActivity(request, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Hub not found");

        verify(hubRepository).findById(999L);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void createActivity_WithInvalidUser_ShouldThrowException() {
        // Given
        CreateActivityRequest request = new CreateActivityRequest();
        request.setHubId(1L);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(testHub));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> activityService.createActivity(request, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(userRepository).findById(999L);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void joinActivity_WithNewParticipation_ShouldCreateParticipation() {
        // Given
        Long activityId = 1L;
        Long userId = 1L;
        Participation.ParticipationStatus status = Participation.ParticipationStatus.INTERESTED;

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(participationRepository.findByUserIdAndActivityId(userId, activityId))
            .thenReturn(Optional.empty());

        // When
        activityService.joinActivity(activityId, userId, status);

        // Then
        verify(activityRepository).findById(activityId);
        verify(userRepository).findById(userId);
        verify(participationRepository).findByUserIdAndActivityId(userId, activityId);
        verify(participationRepository).save(any(Participation.class));
    }

    @Test
    void joinActivity_WithExistingParticipation_ShouldUpdateParticipation() {
        // Given
        Long activityId = 1L;
        Long userId = 1L;
        Participation.ParticipationStatus newStatus = Participation.ParticipationStatus.CONFIRMED;

        Participation existingParticipation = new Participation();
        existingParticipation.setId(1L);
        existingParticipation.setUser(testUser);
        existingParticipation.setActivity(testActivity);
        existingParticipation.setStatus(Participation.ParticipationStatus.INTERESTED);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(participationRepository.findByUserIdAndActivityId(userId, activityId))
            .thenReturn(Optional.of(existingParticipation));

        // When
        activityService.joinActivity(activityId, userId, newStatus);

        // Then
        assertThat(existingParticipation.getStatus()).isEqualTo(newStatus);
        verify(participationRepository).save(existingParticipation);
    }

    @Test
    void joinActivity_WithInvalidActivity_ShouldThrowException() {
        // Given
        Long activityId = 999L;
        Long userId = 1L;

        when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() ->
            activityService.joinActivity(activityId, userId, Participation.ParticipationStatus.INTERESTED))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Activity not found");

        verify(activityRepository).findById(activityId);
        verify(participationRepository, never()).save(any());
    }

    @Test
    void joinActivity_WithInvalidUser_ShouldThrowException() {
        // Given
        Long activityId = 1L;
        Long userId = 999L;

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() ->
            activityService.joinActivity(activityId, userId, Participation.ParticipationStatus.INTERESTED))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
        verify(participationRepository, never()).save(any());
    }
}
