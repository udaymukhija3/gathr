package com.gathr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class FeedbackRequest {

    @NotNull(message = "Activity ID is required")
    private Long activityId;

    @NotNull(message = "Did meet field is required")
    private Boolean didMeet;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer experienceRating;

    private Boolean wouldHangOutAgain;

    private Boolean addedToContacts;

    private String comments;

    // Getters and Setters

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Boolean getDidMeet() {
        return didMeet;
    }

    public void setDidMeet(Boolean didMeet) {
        this.didMeet = didMeet;
    }

    public Integer getExperienceRating() {
        return experienceRating;
    }

    public void setExperienceRating(Integer experienceRating) {
        this.experienceRating = experienceRating;
    }

    public Boolean getWouldHangOutAgain() {
        return wouldHangOutAgain;
    }

    public void setWouldHangOutAgain(Boolean wouldHangOutAgain) {
        this.wouldHangOutAgain = wouldHangOutAgain;
    }

    public Boolean getAddedToContacts() {
        return addedToContacts;
    }

    public void setAddedToContacts(Boolean addedToContacts) {
        this.addedToContacts = addedToContacts;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
