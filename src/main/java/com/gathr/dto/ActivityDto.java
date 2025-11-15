package com.gathr.dto;

import com.gathr.entity.Activity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDto {
    private Long id;
    private String title;
    private Long hubId;
    private String hubName;
    private Activity.ActivityCategory category;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long createdBy;
    private String createdByName;
    private Integer interestedCount;
    private Integer confirmedCount;
    private Integer totalParticipants;
}

