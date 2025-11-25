package com.gathr.service;

import com.gathr.entity.Activity;
import com.gathr.entity.Report;
import com.gathr.entity.User;
import com.gathr.entity.UserRiskScore;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.FeedbackRepository;
import com.gathr.repository.ParticipationRepository;
import com.gathr.repository.ReportRepository;
import com.gathr.repository.UserRepository;
import com.gathr.repository.UserRiskScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fraud and abuse detection service with rule-based analysis.
 *
 * Detection categories:
 * 1. FAKE_ACCOUNT - New accounts with suspicious patterns
 * 2. SPAM_ACTIVITY - Creating too many activities in short time
 * 3. SERIAL_NO_SHOW - Users who repeatedly don't show up
 * 4. HARASSMENT - Multiple reports from different users
 * 5. COORDINATED_ABUSE - Ring detection (future)
 *
 * Actions:
 * - WARN: Add to risk score
 * - THROTTLE: Rate limit user
 * - SOFT_BAN: Restrict features
 * - BAN: Full account suspension
 */
@Service
public class FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);

    // Thresholds for auto-actions
    private static final int REPORTS_FOR_AUTO_REVIEW = 3;
    private static final int DISTINCT_REPORTERS_FOR_AUTO_BAN = 5;
    private static final int NO_SHOW_THRESHOLD = 3;
    private static final int SPAM_ACTIVITY_THRESHOLD = 5; // activities per day
    private static final int RISK_SCORE_SOFT_BAN = 50;
    private static final int RISK_SCORE_AUTO_BAN = 80;

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ActivityRepository activityRepository;
    private final FeedbackRepository feedbackRepository;
    private final ParticipationRepository participationRepository;
    private final UserRiskScoreRepository userRiskScoreRepository;
    private final EventLogService eventLogService;

    public FraudDetectionService(
            UserRepository userRepository,
            ReportRepository reportRepository,
            ActivityRepository activityRepository,
            FeedbackRepository feedbackRepository,
            ParticipationRepository participationRepository,
            UserRiskScoreRepository userRiskScoreRepository,
            EventLogService eventLogService) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.activityRepository = activityRepository;
        this.feedbackRepository = feedbackRepository;
        this.participationRepository = participationRepository;
        this.userRiskScoreRepository = userRiskScoreRepository;
        this.eventLogService = eventLogService;
    }

    /**
     * Evaluate a user's fraud risk and take automatic actions if needed.
     * Called after:
     * - New report submitted against user
     * - Activity created
     * - Participation confirmed
     * - Feedback with no-show submitted
     */
    @Transactional
    public FraudEvaluationResult evaluateUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new FraudEvaluationResult(userId, 0, "USER_NOT_FOUND", false);
        }

        if (Boolean.TRUE.equals(user.getIsBanned())) {
            return new FraudEvaluationResult(userId, 100, "ALREADY_BANNED", false);
        }

        UserRiskScore riskScore = getOrCreateRiskScore(user);

        // Evaluate each risk category
        int reportScore = evaluateReportRisk(userId);
        int behaviorScore = evaluateBehaviorRisk(userId);
        int coordinationScore = 0; // Future: ring detection

        // Update risk scores
        riskScore.setReportScore(reportScore);
        riskScore.setBehaviorScore(behaviorScore);
        riskScore.setCoordinationScore(coordinationScore);

        int totalScore = safe(riskScore.getBaseScore()) + reportScore + behaviorScore + coordinationScore;
        riskScore.setTotalScore(totalScore);
        userRiskScoreRepository.save(riskScore);

        // Determine action
        String action = determineAction(totalScore, userId);
        boolean actionTaken = executeAction(action, user, totalScore);

        // Log evaluation
        logEvaluation(userId, totalScore, action, actionTaken);

        return new FraudEvaluationResult(userId, totalScore, action, actionTaken);
    }

    /**
     * Quick check called on activity creation to detect spam.
     */
    @Transactional
    public boolean checkActivityCreationAllowed(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || Boolean.TRUE.equals(user.getIsBanned())) {
            return false;
        }

        // Check if user is throttled
        UserRiskScore riskScore = userRiskScoreRepository.findById(userId).orElse(null);
        if (riskScore != null && riskScore.getTotalScore() >= RISK_SCORE_SOFT_BAN) {
            logger.warn("User {} is soft-banned, blocking activity creation", userId);
            return false;
        }

        // Check spam rate
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        int recentActivities = activityRepository.countByCreatedByIdAndCreatedAtAfter(userId, oneDayAgo);

        if (recentActivities >= SPAM_ACTIVITY_THRESHOLD) {
            logger.warn("User {} hit activity spam threshold: {} activities in 24h", userId, recentActivities);
            // Add to risk score
            addBehaviorRisk(userId, 10, "SPAM_ACTIVITY");
            return false;
        }

        return true;
    }

    /**
     * Called after a report is submitted to check for auto-actions.
     */
    @Transactional
    public void onReportSubmitted(Long targetUserId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        long recentReports = reportRepository.countRecentReportsByUserId(targetUserId, sevenDaysAgo);
        long distinctReporters = reportRepository.countDistinctReportersByUserId(targetUserId, sevenDaysAgo);

        logger.info("User {} has {} recent reports from {} distinct reporters",
                    targetUserId, recentReports, distinctReporters);

        // Auto-review threshold
        if (recentReports >= REPORTS_FOR_AUTO_REVIEW) {
            flagForReview(targetUserId, "MULTIPLE_REPORTS",
                          Map.of("recentReports", recentReports, "distinctReporters", distinctReporters));
        }

        // Auto-ban threshold (multiple distinct reporters)
        if (distinctReporters >= DISTINCT_REPORTERS_FOR_AUTO_BAN) {
            User user = userRepository.findById(targetUserId).orElse(null);
            if (user != null && !Boolean.TRUE.equals(user.getIsBanned())) {
                logger.warn("Auto-banning user {} due to {} distinct reporters", targetUserId, distinctReporters);
                user.setIsBanned(true);
                userRepository.save(user);

                logEvaluation(targetUserId, 100, "AUTO_BAN", true);
            }
        } else {
            // Re-evaluate risk
            evaluateUser(targetUserId);
        }
    }

    /**
     * Called after feedback with no-show is submitted.
     */
    @Transactional
    public void onNoShowReported(Long userId) {
        int totalNoShows = feedbackRepository.countNoShowsByUserId(userId);

        if (totalNoShows >= NO_SHOW_THRESHOLD) {
            logger.info("User {} has {} no-shows, adding behavior risk", userId, totalNoShows);
            addBehaviorRisk(userId, 15, "SERIAL_NO_SHOW");
            evaluateUser(userId);
        }
    }

    /**
     * Scheduled job to re-evaluate high-risk users daily.
     */
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    @Transactional
    public void dailyRiskEvaluation() {
        logger.info("Starting daily risk evaluation");

        List<UserRiskScore> highRiskUsers = userRiskScoreRepository.findByTotalScoreGreaterThan(30);

        for (UserRiskScore riskScore : highRiskUsers) {
            try {
                evaluateUser(riskScore.getUserId());
            } catch (Exception e) {
                logger.error("Failed to evaluate user {}", riskScore.getUserId(), e);
            }
        }

        logger.info("Daily risk evaluation completed for {} users", highRiskUsers.size());
    }

    // --- Private helper methods ---

    private int evaluateReportRisk(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentReports = reportRepository.countRecentReportsByUserId(userId, thirtyDaysAgo);
        long distinctReporters = reportRepository.countDistinctReportersByUserId(userId, thirtyDaysAgo);

        int score = 0;

        // Base score from report count
        score += (int) Math.min(recentReports * 5, 25);

        // Bonus for distinct reporters (more concerning)
        if (distinctReporters >= 3) {
            score += 15;
        } else if (distinctReporters >= 2) {
            score += 10;
        }

        return score;
    }

    private int evaluateBehaviorRisk(Long userId) {
        int score = 0;

        // No-show rate
        int showUps = feedbackRepository.countShowUpsByUserId(userId);
        int noShows = feedbackRepository.countNoShowsByUserId(userId);
        int totalMeetings = showUps + noShows;

        if (totalMeetings >= 3) {
            double noShowRate = (double) noShows / totalMeetings;
            if (noShowRate > 0.5) {
                score += 20; // Very unreliable
            } else if (noShowRate > 0.3) {
                score += 10; // Somewhat unreliable
            }
        }

        // Activity creation spam (checked elsewhere but factor into overall)
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        int recentActivities = activityRepository.countByCreatedByIdAndCreatedAtAfter(userId, oneDayAgo);
        if (recentActivities > 3) {
            score += 5;
        }

        return score;
    }

    private void addBehaviorRisk(Long userId, int delta, String reason) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        UserRiskScore riskScore = getOrCreateRiskScore(user);
        int newBehaviorScore = safe(riskScore.getBehaviorScore()) + delta;
        riskScore.setBehaviorScore(newBehaviorScore);

        int total = safe(riskScore.getBaseScore()) + newBehaviorScore +
                    safe(riskScore.getReportScore()) + safe(riskScore.getCoordinationScore());
        riskScore.setTotalScore(total);

        userRiskScoreRepository.save(riskScore);

        Map<String, Object> props = new HashMap<>();
        props.put("delta", delta);
        props.put("reason", reason);
        props.put("newTotal", total);
        eventLogService.log(userId, null, "behavior_risk_added", props);
    }

    private String determineAction(int totalScore, Long userId) {
        if (totalScore >= RISK_SCORE_AUTO_BAN) {
            return "BAN";
        } else if (totalScore >= RISK_SCORE_SOFT_BAN) {
            return "SOFT_BAN";
        } else if (totalScore >= 30) {
            return "WARN";
        }
        return "NONE";
    }

    private boolean executeAction(String action, User user, int totalScore) {
        switch (action) {
            case "BAN":
                if (!Boolean.TRUE.equals(user.getIsBanned())) {
                    user.setIsBanned(true);
                    userRepository.save(user);
                    logger.warn("User {} auto-banned with risk score {}", user.getId(), totalScore);
                    return true;
                }
                break;
            case "SOFT_BAN":
                // Soft ban is enforced via risk score check, no user field change needed
                logger.info("User {} soft-banned with risk score {}", user.getId(), totalScore);
                return true;
            case "WARN":
                logger.info("User {} warned with risk score {}", user.getId(), totalScore);
                return true;
            default:
                break;
        }
        return false;
    }

    private void flagForReview(Long userId, String reason, Map<String, Object> details) {
        Map<String, Object> props = new HashMap<>(details);
        props.put("reason", reason);
        eventLogService.log(userId, null, "flagged_for_review", props);
        logger.info("User {} flagged for review: {}", userId, reason);
    }

    private void logEvaluation(Long userId, int totalScore, String action, boolean actionTaken) {
        Map<String, Object> props = new HashMap<>();
        props.put("totalScore", totalScore);
        props.put("action", action);
        props.put("actionTaken", actionTaken);
        eventLogService.log(userId, null, "fraud_evaluation", props);
    }

    private UserRiskScore getOrCreateRiskScore(User user) {
        return userRiskScoreRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserRiskScore rs = new UserRiskScore();
                    rs.setUser(user);
                    rs.setUserId(user.getId());
                    return userRiskScoreRepository.save(rs);
                });
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * Result of a fraud evaluation.
     */
    public record FraudEvaluationResult(
            Long userId,
            int riskScore,
            String action,
            boolean actionTaken
    ) {}
}
