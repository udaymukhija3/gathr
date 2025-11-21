package com.gathr.dto;

import com.gathr.entity.Activity;

public record ActivityTemplateDto(
    Long id,
    String name,
    String title,
    Activity.ActivityCategory category,
    Integer durationHours,
    String description,
    Boolean isSystemTemplate,
    Boolean isInviteOnly,
    Integer maxMembers
) {}
