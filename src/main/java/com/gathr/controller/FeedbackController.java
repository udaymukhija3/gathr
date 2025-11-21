package com.gathr.controller;

import com.gathr.dto.FeedbackRequest;
import com.gathr.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        feedbackService.submitFeedback(request, userId);
        return ResponseEntity.ok(new MessageResponse("Feedback submitted successfully"));
    }

    @GetMapping("/check")
    public ResponseEntity<HasFeedbackResponse> checkFeedback(
            @RequestParam Long activityId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        boolean hasFeedback = feedbackService.hasFeedback(userId, activityId);
        return ResponseEntity.ok(new HasFeedbackResponse(hasFeedback));
    }

    private record MessageResponse(String message) {}
    private record HasFeedbackResponse(boolean hasFeedback) {}
}
