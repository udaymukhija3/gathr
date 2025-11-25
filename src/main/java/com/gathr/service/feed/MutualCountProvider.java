package com.gathr.service.feed;

/**
 * Abstraction used by {@link FeedScoringEngine} to obtain mutual-friend counts
 * without depending directly on the full {@link com.gathr.service.SocialGraphService}.
 */
@FunctionalInterface
public interface MutualCountProvider {
    int getMutualCount(Long userId, Long activityId);
}

