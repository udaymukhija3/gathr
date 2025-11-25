package com.gathr.service.feed;

import com.gathr.dto.TrustScoreDto;

/**
 * Thin abstraction for retrieving trust scores during feed scoring.
 */
@FunctionalInterface
public interface TrustScoreProvider {
    TrustScoreDto calculate(Long userId);
}

