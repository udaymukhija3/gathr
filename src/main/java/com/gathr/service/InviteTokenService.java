package com.gathr.service;

import com.gathr.entity.InviteToken;
import com.gathr.entity.Activity;
import com.gathr.entity.User;
import com.gathr.repository.InviteTokenRepository;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.UserRepository;
import com.gathr.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InviteTokenService {

    private final InviteTokenRepository inviteTokenRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final EventLogService eventLogService;

    public InviteTokenService(
            InviteTokenRepository inviteTokenRepository,
            ActivityRepository activityRepository,
            UserRepository userRepository,
            EventLogService eventLogService) {
        this.inviteTokenRepository = inviteTokenRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.eventLogService = eventLogService;
    }

    @Transactional
    public InviteToken generateInviteToken(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Generate unique token
        String token = UUID.randomUUID().toString().replace("-", "");

        // Create invite token (expires in 48 hours by default)
        InviteToken inviteToken = new InviteToken();
        inviteToken.setActivity(activity);
        inviteToken.setToken(token);
        inviteToken.setCreatedBy(user);
        inviteToken.setExpiresAt(LocalDateTime.now().plusHours(48));
        inviteToken.setUseCount(0);
        inviteToken.setRevoked(false);

        inviteToken = inviteTokenRepository.save(inviteToken);

        // Log event
        java.util.Map<String, Object> eventProps = new java.util.HashMap<>();
        eventProps.put("inviteTokenId", inviteToken.getId());
        eventProps.put("token", token);
        eventLogService.log(userId, activityId, "invite_token_generated", eventProps);

        return inviteToken;
    }

    @Transactional(readOnly = true)
    public boolean isValidToken(String token) {
        return inviteTokenRepository.findValidToken(token, LocalDateTime.now()).isPresent();
    }

    @Transactional
    public InviteToken validateAndUseToken(String token) {
        InviteToken inviteToken = inviteTokenRepository.findValidToken(token, LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("InviteToken", "token", token));

        // Increment use count
        inviteToken.incrementUseCount();
        return inviteTokenRepository.save(inviteToken);
    }
}

