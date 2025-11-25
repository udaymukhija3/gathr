package com.gathr.controller;

import com.gathr.dto.FeedbackDto;
import com.gathr.dto.FeedbackRequest;
import com.gathr.dto.FeedbackStatsDto;
import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AuthenticatedUserService authenticatedUserService;

    public FeedbackController(FeedbackService feedbackService, AuthenticatedUserService authenticatedUserService) {
        this.feedbackService = feedbackService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping
    public ResponseEntity<FeedbackDto> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        FeedbackDto feedback = feedbackService.submitFeedback(request, userId);
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/check")
    public ResponseEntity<HasFeedbackResponse> checkFeedback(
            @RequestParam Long activityId,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        boolean hasFeedback = feedbackService.hasFeedback(userId, activityId);
        return ResponseEntity.ok(new HasFeedbackResponse(hasFeedback));
    }

    @GetMapping("/me")
    public ResponseEntity<List<FeedbackDto>> getMyFeedbacks(Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        List<FeedbackDto> feedbacks = feedbackService.getFeedbacksByUser(userId);
        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<List<FeedbackDto>> getFeedbacksByActivity(@PathVariable Long activityId) {
        List<FeedbackDto> feedbacks = feedbackService.getFeedbacksByActivity(activityId);
        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/activities/{activityId}/stats")
    public ResponseEntity<FeedbackStatsDto> getActivityStats(@PathVariable Long activityId) {
        FeedbackStatsDto stats = feedbackService.getActivityStats(activityId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/activities/{activityId}/mine")
    public ResponseEntity<FeedbackDto> getMyFeedbackForActivity(
            @PathVariable Long activityId,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        FeedbackDto feedback = feedbackService.getFeedback(userId, activityId);
        return ResponseEntity.ok(feedback);
    }

    private record HasFeedbackResponse(boolean hasFeedback) {}
}
