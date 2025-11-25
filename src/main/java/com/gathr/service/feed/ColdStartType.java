package com.gathr.service.feed;

/**
 * Profiles describing a user's familiarity and recency with the product/hub.
 * Used to adjust recommendation weights for cold-start scenarios.
 */
public enum ColdStartType {
    NONE,
    NEW_USER_NO_INTERESTS,
    NEW_USER_WITH_INTERESTS,
    RETURNING_USER_NEW_HUB,
    INACTIVE_USER_RETURNING
}

