package com.gathr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "did_meet", nullable = false)
    private Boolean didMeet;

    @Column(name = "experience_rating")
    private Integer experienceRating; // 1-5 stars

    @Column(name = "would_hang_out_again")
    private Boolean wouldHangOutAgain;

    @Column(name = "added_to_contacts")
    private Boolean addedToContacts = false;

    @Column(name = "comments")
    private String comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
