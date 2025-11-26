package com.gathr.recs;

import com.gathr.dto.ActivitySuggestionDto;
import com.gathr.entity.Activity.ActivityCategory;
import com.gathr.entity.User;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CreationAssistant {

    /**
     * Suggests activities for a user to host based on time of day and day of week.
     * This is a Heuristic V1 implementation.
     */
    public List<ActivitySuggestionDto> suggestActivities(User user) {
        List<ActivitySuggestionDto> suggestions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalTime time = now.toLocalTime();
        DayOfWeek day = now.getDayOfWeek();
        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;

        // Morning (5 AM - 11 AM)
        if (time.isAfter(LocalTime.of(5, 0)) && time.isBefore(LocalTime.of(11, 0))) {
            suggestions.add(ActivitySuggestionDto.builder()
                    .category(ActivityCategory.WELLNESS)
                    .title("Morning Yoga / Meditation")
                    .suggestedTime("07:00")
                    .reason("Great way to start the day")
                    .confidenceScore(0.85)
                    .build());

            suggestions.add(ActivitySuggestionDto.builder()
                    .category(ActivityCategory.OUTDOOR)
                    .title("Morning Walk / Run")
                    .suggestedTime("06:30")
                    .reason("Popular in your area this morning")
                    .confidenceScore(0.80)
                    .build());
        }

        // Lunch / Afternoon (11 AM - 4 PM)
        else if (time.isAfter(LocalTime.of(11, 0)) && time.isBefore(LocalTime.of(16, 0))) {
            suggestions.add(ActivitySuggestionDto.builder()
                    .category(ActivityCategory.FOOD)
                    .title("Lunch Meetup")
                    .suggestedTime("13:00")
                    .reason("People are looking for lunch buddies")
                    .confidenceScore(0.90)
                    .build());

            if (isWeekend) {
                suggestions.add(ActivitySuggestionDto.builder()
                        .category(ActivityCategory.ART)
                        .title("Afternoon Art Jam")
                        .suggestedTime("14:00")
                        .reason("Perfect for a lazy weekend afternoon")
                        .confidenceScore(0.75)
                        .build());
            }
        }

        // Evening (4 PM - 9 PM)
        else if (time.isAfter(LocalTime.of(16, 0)) && time.isBefore(LocalTime.of(21, 0))) {
            suggestions.add(ActivitySuggestionDto.builder()
                    .category(ActivityCategory.SPORTS)
                    .title("Badminton / Tennis")
                    .suggestedTime("18:00")
                    .reason("Courts are filling up fast!")
                    .confidenceScore(0.95)
                    .build());

            suggestions.add(ActivitySuggestionDto.builder()
                    .category(ActivityCategory.FOOD)
                    .title("Dinner & Drinks")
                    .suggestedTime("20:00")
                    .reason("Classic evening choice")
                    .confidenceScore(0.85)
                    .build());
        }

        // Late Night (9 PM - 5 AM)
        else {
            suggestions.add(ActivitySuggestionDto.builder()
                    .category(ActivityCategory.FOOD)
                    .title("Late Night Snack")
                    .suggestedTime("22:00")
                    .reason("For the night owls")
                    .confidenceScore(0.60)
                    .build());
        }

        return suggestions;
    }
}
