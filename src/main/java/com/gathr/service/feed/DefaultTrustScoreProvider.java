package com.gathr.service.feed;

import com.gathr.dto.TrustScoreDto;
import com.gathr.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DefaultTrustScoreProvider implements TrustScoreProvider {

    private final TrustScoreService trustScoreService;

    @Override
    public TrustScoreDto calculate(Long userId) {
        return trustScoreService.calculateTrustScore(userId);
    }
}

