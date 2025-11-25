package com.gathr.service;

import com.gathr.entity.Activity;
import com.gathr.entity.Participation;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.ParticipationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for sending activity reminder notifications.
 * Sends reminders 30 minutes before activity start time.
 */
@Service
public class ActivityReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ActivityReminderScheduler.class);
    private static final int REMINDER_MINUTES_BEFORE = 30;

    private final ActivityRepository activityRepository;
    private final ParticipationRepository participationRepository;
    private final NotificationService notificationService;

    public ActivityReminderScheduler(
            ActivityRepository activityRepository,
            ParticipationRepository participationRepository,
            NotificationService notificationService) {
        this.activityRepository = activityRepository;
        this.participationRepository = participationRepository;
        this.notificationService = notificationService;
    }

    /**
     * Run every 5 minutes to check for activities starting soon.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void sendActivityReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindow = now.plusMinutes(REMINDER_MINUTES_BEFORE);

        logger.debug("Checking for activities starting between {} and {}", now, reminderWindow);

        // Find activities starting in the next 30 minutes
        List<Activity> upcomingActivities = activityRepository.findActivitiesStartingBetween(
                now.plusMinutes(REMINDER_MINUTES_BEFORE - 5), // Window start
                reminderWindow // Window end
        );

        for (Activity activity : upcomingActivities) {
            sendRemindersForActivity(activity);
        }

        if (!upcomingActivities.isEmpty()) {
            logger.info("Sent reminders for {} activities", upcomingActivities.size());
        }
    }

    private void sendRemindersForActivity(Activity activity) {
        // Get confirmed participants
        List<Participation> participants = participationRepository.findByActivityIdAndStatus(
                activity.getId(),
                Participation.ParticipationStatus.CONFIRMED
        );

        logger.debug("Sending reminders for activity {} to {} participants",
                activity.getId(), participants.size());

        for (Participation participation : participants) {
            try {
                notificationService.sendActivityReminder(participation.getUser(), activity);
            } catch (Exception e) {
                logger.error("Failed to send reminder to user {} for activity {}: {}",
                        participation.getUser().getId(), activity.getId(), e.getMessage());
            }
        }
    }
}
