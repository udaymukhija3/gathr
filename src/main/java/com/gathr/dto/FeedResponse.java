package com.gathr.dto;

import com.gathr.service.FeedMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponse {
    private List<ScoredActivityDto> activities;
    private Long hubId;
    private LocalDate date;
    private Integer totalCount;
    private Long userId;
    private Boolean fallbackUsed;
    private List<String> suggestions;
    private FeedMeta meta;
}

