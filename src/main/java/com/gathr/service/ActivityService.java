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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    // Configurable via application.properties: activity.identity-reveal-threshold
    @Value("${activity.identity-reveal-threshold:3}")
    private int identityRevealThreshold;

    @Value("${activity.default-max-members:4}")
    private int defaultMaxMembers;

    private final ActivityRepository activityRepository;
    private final HubRepository hubRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final InviteTokenService inviteTokenService;
    private final EventLogService eventLogService;
    private final ActivityMetricsService activityMetricsService;
    private final SocialGraphService socialGraphService;
    
    public ActivityService(ActivityRepository activityRepository,
                          HubRepository hubRepository,
                          UserRepository userRepository,
                          ParticipationRepository participationRepository,
                          InviteTokenService inviteTokenService,
                          EventLogService eventLogService,
                          ActivityMetricsService activityMetricsService,
                          SocialGraphService socialGraphService) {
        this.activityRepository = activityRepository;
        this.hubRepository = hubRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.inviteTokenService = inviteTokenService;
        this.eventLogService = eventLogService;
        this.activityMetricsService = activityMetricsService;
        this.socialGraphService = socialGraphService;
    }
    
    @Transactional(readOnly = true)
    public List<ActivityDto> getActivitiesByHub(Long hubId) {
        LocalDate today = LocalDate.now();
        List<Activity> activities = activityRepository.findByHubIdAndDate(hubId, today);
        return activities.stream()
                .map(activity -> convertToDto(activity, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityDto> getActivitiesNearby(double latitude, double longitude, double radiusKm) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Activity> activities = activityRepository.findActivitiesStartingBetween(start, end);

        return activities.stream()
                .filter(activity -> computeDistanceKm(activity, latitude, longitude) <= radiusKm)
                .map(activity -> {
                    ActivityDto dto = convertToDto(activity, true);
                    dto.setDistanceKm(computeDistanceKm(activity, latitude, longitude));
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ActivityDto createActivity(CreateActivityRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Hub hub = null;
        if (request.getHubId() != null) {
            hub = hubRepository.findById(request.getHubId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hub", request.getHubId()));
        } else if (!request.hasValidCustomLocation()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either a hubId or a valid custom location is required");
        }
        
        Activity activity = new Activity();
        activity.setTitle(request.getTitle());
        activity.setCategory(request.getCategory());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setCreatedBy(user);
        activity.setIsInviteOnly(request.getIsInviteOnly() != null ? request.getIsInviteOnly() : false);
        activity.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : defaultMaxMembers);

        if (hub != null) {
            activity.setHub(hub);
            activity.setIsUserLocation(false);
            activity.setPlaceName(hub.getName());
            activity.setPlaceAddress(hub.getArea());
            if (hub.getLatitude() != null) {
                activity.setLatitude(hub.getLatitude().doubleValue());
            }
            if (hub.getLongitude() != null) {
                activity.setLongitude(hub.getLongitude().doubleValue());
            }
        } else {
            activity.setHub(null);
            activity.setIsUserLocation(true);
            activity.setPlaceId(request.getPlaceId());
            activity.setPlaceName(request.getPlaceName());
            activity.setPlaceAddress(request.getPlaceAddress());
            activity.setLatitude(request.getLatitude());
            activity.setLongitude(request.getLongitude());
        }
        
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
                            if (status == Participation.ParticipationStatus.CONFIRMED) {
                                socialGraphService.refreshConnectionsForActivity(activityId);
                            }
                    },
                    () -> {
                        Participation participation = new Participation();
                        participation.setUser(user);
                        participation.setActivity(activity);
                        participation.setStatus(status);
                        participationRepository.save(participation);
                            if (status == Participation.ParticipationStatus.CONFIRMED) {
                                socialGraphService.refreshConnectionsForActivity(activityId);
                            }
                    }
                );

        // Check and update identity reveal status
        checkAndRevealIdentities(activity);

        // Update activity metrics
        activityMetricsService.recordJoin(activityId);

        // Log event
        Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("status", status.name());
        eventLogService.log(userId, activityId, "activity_joined", eventProps);
    }

    @Transactional
    public void confirmActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));

        userRepository.findById(userId)
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

        // Check and update identity reveal status
        checkAndRevealIdentities(activity);

        // Log event
        Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("status", "CONFIRMED");
        eventLogService.log(userId, activityId, "activity_confirmed", eventProps);

        socialGraphService.refreshConnectionsForActivity(activityId);
    }

    /**
     * Check if identity reveal threshold is met and update activity if so.
     * Identities are revealed only when enough participants have CONFIRMED attendance.
     */
    private void checkAndRevealIdentities(Activity activity) {
        if (activity.getRevealIdentities()) {
            return; // Already revealed
        }

        int confirmedCount = participationRepository.countByActivityIdAndStatus(
                activity.getId(), Participation.ParticipationStatus.CONFIRMED);

        if (confirmedCount >= identityRevealThreshold) {
            activity.setRevealIdentities(true);
            activityRepository.save(activity);
        }
    }

    private double computeDistanceKm(Activity activity, double latitude, double longitude) {
        Double actLat = activity.getLatitude();
        Double actLng = activity.getLongitude();

        if (actLat == null || actLng == null) {
            Hub hub = activity.getHub();
            if (hub == null || hub.getLatitude() == null || hub.getLongitude() == null) {
                return Double.MAX_VALUE;
            }
            actLat = hub.getLatitude().doubleValue();
            actLng = hub.getLongitude().doubleValue();
        }

        return haversine(actLat, actLng, latitude, longitude);
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(rLat1) * Math.cos(rLat2) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public ActivityDto convertToDto(Activity activity, boolean includeParticipantCounts) {
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

        Long hubId = activity.getHub() != null ? activity.getHub().getId() : null;
        String hubName = activity.getHub() != null ? activity.getHub().getName() : null;
        ActivityDto dto = new ActivityDto(
                activity.getId(),
                activity.getTitle(),
                hubId,
                hubName,
                activity.getPlaceName() != null ? activity.getPlaceName() : hubName,
                activity.getPlaceAddress(),
                activity.getPlaceId(),
                activity.getLatitude(),
                activity.getLongitude(),
                activity.getIsUserLocation(),
                null,
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

