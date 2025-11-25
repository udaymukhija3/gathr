package com.gathr.service.feed;

import com.gathr.service.SocialGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DefaultMutualCountProvider implements MutualCountProvider {

    private final SocialGraphService socialGraphService;

    @Override
    public int getMutualCount(Long userId, Long activityId) {
        return socialGraphService.getMutualCountForActivity(userId, activityId);
    }
}

