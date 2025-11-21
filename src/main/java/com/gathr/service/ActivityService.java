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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    private final HubRepository hubRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final InviteTokenService inviteTokenService;
    private final EventLogService eventLogService;
    
    public ActivityService(ActivityRepository activityRepository,
                          HubRepository hubRepository,
                          UserRepository userRepository,
                          ParticipationRepository participationRepository,
                          InviteTokenService inviteTokenService,
                          EventLogService eventLogService) {
        this.activityRepository = activityRepository;
        this.hubRepository = hubRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.inviteTokenService = inviteTokenService;
        this.eventLogService = eventLogService;
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
        activity.setIsInviteOnly(request.getIsInviteOnly() != null ? request.getIsInviteOnly() : false);
        activity.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 4);
        
        activity = activityRepository.save(activity);

        // Log event
        Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("activityId", activity.getId());
        eventProps.put("title", activity.getTitle());
        eventProps.put("isInviteOnly", activity.getIsInviteOnly());
        eventLogService.log(userId, activity.getId(), "activity_created", eventProps);

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
    public void joinActivity(Long activityId, Long userId, Participation.ParticipationStatus status, String inviteToken) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check if activity is invite-only
        if (Boolean.TRUE.equals(activity.getIsInviteOnly())) {
            if (inviteToken == null || inviteToken.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invite token required for invite-only activities");
            }
            // Validate invite token
            if (!inviteTokenService.isValidToken(inviteToken)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired invite token");
            }
            // Use the token (increment use count)
            inviteTokenService.validateAndUseToken(inviteToken);
        }

        // Check max members limit
        int currentParticipants = participationRepository
                .countByActivityIdAndStatusIn(activityId, 
                    List.of(Participation.ParticipationStatus.INTERESTED, Participation.ParticipationStatus.CONFIRMED));
        
        if (currentParticipants >= activity.getMaxMembers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Activity has reached maximum number of participants");
        }
        
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

        // Check if we should reveal identities (>=3 confirmed OR >=3 interested)
        int confirmedCount = participationRepository.countByActivityIdAndStatus(
                activityId, Participation.ParticipationStatus.CONFIRMED);
        int interestedCount = participationRepository.countByActivityIdAndStatus(
                activityId, Participation.ParticipationStatus.INTERESTED);

        if (!activity.getRevealIdentities() && (confirmedCount >= 3 || interestedCount >= 3)) {
            activity.setRevealIdentities(true);
            activityRepository.save(activity);
        }

        // Log event
        Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("status", status.name());
        eventLogService.log(userId, activityId, "activity_joined", eventProps);
    }

    @Transactional
    public void confirmActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Find existing participation or throw error
        Participation participation = participationRepository.findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User has not joined this activity yet"
                ));

        // Update status to CONFIRMED
        participation.setStatus(Participation.ParticipationStatus.CONFIRMED);
        participationRepository.save(participation);

        // Check if we should reveal identities (>=3 confirmed OR >=3 interested)
        int confirmedCount = participationRepository.countByActivityIdAndStatus(
                activityId, Participation.ParticipationStatus.CONFIRMED);
        int interestedCount = participationRepository.countByActivityIdAndStatus(
                activityId, Participation.ParticipationStatus.INTERESTED);

        if (!activity.getRevealIdentities() && (confirmedCount >= 3 || interestedCount >= 3)) {
            activity.setRevealIdentities(true);
            activityRepository.save(activity);
        }

        // Log event
        Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("status", "CONFIRMED");
        eventLogService.log(userId, activityId, "activity_confirmed", eventProps);
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

        ActivityDto dto = new ActivityDto(
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
            totalParticipants,
            totalParticipants, // peopleCount
            null, // mutualsCount - will be set by controller if needed
            activity.getIsInviteOnly(),
            activity.getRevealIdentities(),
            activity.getMaxMembers()
        );
        return dto;
    }
}

