package com.gathr.service;

import com.gathr.dto.ScoredActivityDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class FeedComputationResult {
    private List<ScoredActivityDto> activities;
    private boolean fallbackUsed;
    private List<String> suggestions;
    private FeedMeta feedMeta;
}

