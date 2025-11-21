package com.gathr.service;

import com.gathr.entity.Report;
import com.gathr.entity.User;
import com.gathr.entity.Activity;
import com.gathr.repository.ReportRepository;
import com.gathr.repository.UserRepository;
import com.gathr.repository.ActivityRepository;
import com.gathr.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final EventLogService eventLogService;
    private final WebClient webClient;
    private final String slackWebhookUrl;

    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ActivityRepository activityRepository,
            EventLogService eventLogService,
            WebClient.Builder webClientBuilder,
            @Value("${slack.webhook.url:}") String slackWebhookUrl) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.eventLogService = eventLogService;
        this.webClient = webClientBuilder.build();
        this.slackWebhookUrl = slackWebhookUrl;
    }

    @Transactional
    public Report createReport(Long reporterId, Long targetUserId, Long activityId, String reason) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));
        
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        Activity activity = null;
        if (activityId != null) {
            activity = activityRepository.findById(activityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Activity", activityId));
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetUser(targetUser);
        report.setActivity(activity);
        report.setReason(reason);
        report.setStatus("OPEN");

        report = reportRepository.save(report);

        // Log event
        Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("reportId", report.getId());
        eventProps.put("targetUserId", targetUserId);
        eventProps.put("activityId", activityId);
        eventProps.put("reason", reason);
        eventLogService.log(reporterId, activityId, "report_created", eventProps);

        // Send Slack notification (async, non-blocking)
        sendSlackNotification(report);

        return report;
    }

    private void sendSlackNotification(Report report) {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            logger.warn("Slack webhook URL not configured, skipping notification");
            return;
        }

        Map<String, Object> slackPayload = new HashMap<>();
        slackPayload.put("text", "New Report Created");
        
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", "warning");
        attachment.put("title", "Report #" + report.getId());
        
        StringBuilder fields = new StringBuilder();
        fields.append("Reporter ID: ").append(report.getReporter().getId()).append("\n");
        fields.append("Target User ID: ").append(report.getTargetUser().getId()).append("\n");
        if (report.getActivity() != null) {
            fields.append("Activity ID: ").append(report.getActivity().getId()).append("\n");
        }
        fields.append("Reason: ").append(report.getReason()).append("\n");
        fields.append("Status: ").append(report.getStatus()).append("\n");
        fields.append("Created At: ").append(report.getCreatedAt()).append("\n");
        
        attachment.put("text", fields.toString());
        slackPayload.put("attachments", new Object[]{attachment});

        webClient.post()
                .uri(slackWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(slackPayload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.info("Slack notification sent successfully for report {}", report.getId()))
                .doOnError(error -> logger.error("Failed to send Slack notification for report {}", report.getId(), error))
                .onErrorResume(error -> {
                    logger.error("Error sending Slack notification", error);
                    return Mono.empty();
                })
                .subscribe();
    }
}

