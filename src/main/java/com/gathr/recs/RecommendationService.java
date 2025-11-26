package com.gathr.recs;

import com.gathr.dto.ScoredActivityDto;
import com.gathr.entity.Activity;
import com.gathr.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    /**
     * Rank a list of candidate activities for a specific user.
     * Currently uses a placeholder heuristic.
     * In Phase 3, this will call an ML model.
     */
    public List<ScoredActivityDto> rankFeed(User user, List<Activity> candidates) {
        // Placeholder: In the future, this will use XGBoost or similar.
        // For now, we rely on the FeedService's existing logic or pass through.
        // This method is prepared for the architecture refactor.

        return candidates.stream()
                .map(activity -> ScoredActivityDto.builder()
                        .activity(null) // We would map to DTO here
                        .score(0.5) // Default score
                        .build())
                .toList();
    }
}
