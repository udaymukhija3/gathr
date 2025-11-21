package com.gathr.dto;

import com.gathr.entity.Activity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Category is required")
    private Activity.ActivityCategory category;

    private Integer durationHours;

    private String description;

    private Boolean isInviteOnly;

    private Integer maxMembers;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Activity.ActivityCategory getCategory() {
        return category;
    }

    public void setCategory(Activity.ActivityCategory category) {
        this.category = category;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsInviteOnly() {
        return isInviteOnly;
    }

    public void setIsInviteOnly(Boolean isInviteOnly) {
        this.isInviteOnly = isInviteOnly;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }
}
