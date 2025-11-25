package com.gathr.service;

import com.gathr.dto.FeedbackDto;
import com.gathr.dto.FeedbackRequest;
import com.gathr.dto.FeedbackStatsDto;
import com.gathr.entity.Activity;
import com.gathr.entity.Feedback;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.FeedbackRepository;
import com.gathr.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final EventLogService eventLogService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                          ActivityRepository activityRepository,
                          UserRepository userRepository,
                          EventLogService eventLogService) {
        this.feedbackRepository = feedbackRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.eventLogService = eventLogService;
    }

    @Transactional
    public FeedbackDto submitFeedback(FeedbackRequest request, Long userId) {
        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new ResourceNotFoundException("Activity", request.getActivityId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check if feedback already exists
        feedbackRepository.findByUserIdAndActivityId(userId, request.getActivityId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Feedback already submitted for this activity"
                    );
                });

        Feedback feedback = new Feedback();
        feedback.setActivity(activity);
        feedback.setUser(user);
        feedback.setDidMeet(request.getDidMeet());
        feedback.setExperienceRating(request.getExperienceRating());
        feedback.setWouldHangOutAgain(request.getWouldHangOutAgain());
        feedback.setAddedToContacts(request.getAddedToContacts() != null ? request.getAddedToContacts() : false);
        feedback.setComments(request.getComments());

        feedback = feedbackRepository.save(feedback);

        // Log event
        Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("didMeet", request.getDidMeet());
        eventProps.put("experienceRating", request.getExperienceRating());
        eventProps.put("wouldHangOutAgain", request.getWouldHangOutAgain());
        eventProps.put("addedToContacts", request.getAddedToContacts());
        eventLogService.log(userId, request.getActivityId(), "feedback_submitted", eventProps);

        return FeedbackDto.from(feedback);
    }

    @Transactional(readOnly = true)
    public boolean hasFeedback(Long userId, Long activityId) {
        return feedbackRepository.findByUserIdAndActivityId(userId, activityId).isPresent();
    }

    @Transactional(readOnly = true)
    public FeedbackDto getFeedback(Long userId, Long activityId) {
        return feedbackRepository.findByUserIdAndActivityId(userId, activityId)
                .map(FeedbackDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found for userId=" + userId + ", activityId=" + activityId));
    }

    @Transactional(readOnly = true)
    public List<FeedbackDto> getFeedbacksByActivity(Long activityId) {
        // Verify activity exists
        activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));

        return feedbackRepository.findByActivityId(activityId).stream()
                .map(FeedbackDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedbackDto> getFeedbacksByUser(Long userId) {
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return feedbackRepository.findByUserId(userId).stream()
                .map(FeedbackDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeedbackStatsDto getActivityStats(Long activityId) {
        // Verify activity exists
        activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));

        int totalFeedbacks = feedbackRepository.countByActivityId(activityId);
        int showedUpCount = feedbackRepository.countDidMeetByActivityId(activityId);
        Double averageRating = feedbackRepository.averageRatingByActivityId(activityId);
        int wouldHangOutAgainCount = feedbackRepository.countWouldHangOutAgainByActivityId(activityId);
        int addedToContactsCount = feedbackRepository.countAddedToContactsByActivityId(activityId);

        return FeedbackStatsDto.create(
            activityId,
            totalFeedbacks,
            showedUpCount,
            averageRating,
            wouldHangOutAgainCount,
            addedToContactsCount
        );
    }
}
