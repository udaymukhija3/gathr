package com.gathr.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedMeta {
    private String ctaText;
    private String scarcityMessage;
    private String timeWindowLabel;
    private List<Long> topActivityIds;
}

