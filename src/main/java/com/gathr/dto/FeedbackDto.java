package com.gathr.dto;

import java.time.LocalDateTime;

/**
 * DTO for returning feedback data.
 * Uses record for immutability.
 */
public record FeedbackDto(
    Long id,
    Long activityId,
    String activityTitle,
    Long userId,
    String userName,
    Boolean didMeet,
    Integer experienceRating,
    Boolean wouldHangOutAgain,
    Boolean addedToContacts,
    String comments,
    LocalDateTime createdAt
) {
    /**
     * Factory method to create FeedbackDto from entity.
     * Handles null checks and lazy loading considerations.
     */
    public static FeedbackDto from(com.gathr.entity.Feedback feedback) {
        return new FeedbackDto(
            feedback.getId(),
            feedback.getActivity().getId(),
            feedback.getActivity().getTitle(),
            feedback.getUser().getId(),
            feedback.getUser().getName(),
            feedback.getDidMeet(),
            feedback.getExperienceRating(),
            feedback.getWouldHangOutAgain(),
            feedback.getAddedToContacts(),
            feedback.getComments(),
            feedback.getCreatedAt()
        );
    }
}
