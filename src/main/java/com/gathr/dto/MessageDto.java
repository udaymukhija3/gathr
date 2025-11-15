package com.gathr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private Long activityId;
    private Long userId;
    private String userName;
    private String text;
    private LocalDateTime createdAt;
}

