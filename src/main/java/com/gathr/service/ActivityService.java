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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    private final HubRepository hubRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    
    public ActivityService(ActivityRepository activityRepository,
                          HubRepository hubRepository,
                          UserRepository userRepository,
                          ParticipationRepository participationRepository) {
        this.activityRepository = activityRepository;
        this.hubRepository = hubRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
    }
    
    @Transactional(readOnly = true)
    public List<ActivityDto> getActivitiesByHub(Long hubId) {
        LocalDate today = LocalDate.now();
        List<Activity> activities = activityRepository.findByHubIdAndDate(hubId, today);
        return activities.stream()
                .map(activity -> convertToDto(activity, true))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ActivityDto createActivity(CreateActivityRequest request, Long userId) {
        Hub hub = hubRepository.findById(request.getHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Hub", request.getHubId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        Activity activity = new Activity();
        activity.setTitle(request.getTitle());
        activity.setHub(hub);
        activity.setCategory(request.getCategory());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setCreatedBy(user);
        
        activity = activityRepository.save(activity);
        // Hub and user are already loaded, so we can safely access them
        return convertToDto(activity, false);
    }

    @Transactional(readOnly = true)
    public ActivityDto getActivityById(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));
        return convertToDto(activity, true);
    }

    @Transactional
    public void joinActivity(Long activityId, Long userId, Participation.ParticipationStatus status) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // Check if participation already exists
        participationRepository.findByUserIdAndActivityId(userId, activityId)
                .ifPresentOrElse(
                    existing -> {
                        existing.setStatus(status);
                        participationRepository.save(existing);
                    },
                    () -> {
                        Participation participation = new Participation();
                        participation.setUser(user);
                        participation.setActivity(activity);
                        participation.setStatus(status);
                        participationRepository.save(participation);
                    }
                );
    }
    
    private ActivityDto convertToDto(Activity activity, boolean includeParticipantCounts) {
        Integer interestedCount = null;
        Integer confirmedCount = null;
        Integer totalParticipants = null;

        if (includeParticipantCounts) {
            interestedCount = participationRepository
                    .countByActivityIdAndStatus(activity.getId(), Participation.ParticipationStatus.INTERESTED);
            confirmedCount = participationRepository
                    .countByActivityIdAndStatus(activity.getId(), Participation.ParticipationStatus.CONFIRMED);
            totalParticipants = interestedCount + confirmedCount;
        }

        return new ActivityDto(
            activity.getId(),
            activity.getTitle(),
            activity.getHub().getId(),
            activity.getHub().getName(),
            activity.getCategory(),
            activity.getStartTime(),
            activity.getEndTime(),
            activity.getCreatedBy().getId(),
            activity.getCreatedBy().getName(),
            interestedCount,
            confirmedCount,
            totalParticipants
        );
    }
}

