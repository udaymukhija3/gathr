package com.gathr.service.feed;

import com.gathr.dto.ActivityDto;
import com.gathr.entity.Activity;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of all inputs required to score an activity for a user.
 */
@Builder
public record FeedScoringContext(
        Long userId,
        Activity activity,
        ActivityDto activityDto,
        List<String> userInterests,
        LocalDateTime now,
        ColdStartType coldStartType,
        LocationContext locationContext,
        int preferredHour,
        Map<Activity.ActivityCategory, Long> successCounts,
        int spotsRemaining
) {
}

