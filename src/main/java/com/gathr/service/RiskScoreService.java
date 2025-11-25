package com.gathr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathr.entity.DetectionRule;
import com.gathr.entity.User;
import com.gathr.entity.UserRiskScore;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.DetectionRuleRepository;
import com.gathr.repository.UserRepository;
import com.gathr.repository.UserRiskScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * Minimal rule-based risk scoring service.
 *
 * Phase 4 goal: establish a framework where rules can contribute to a user's risk score,
 * and feature gating can consult that score.
 */
@Service
public class RiskScoreService {

    private static final Logger logger = LoggerFactory.getLogger(RiskScoreService.class);

    private final UserRepository userRepository;
    private final DetectionRuleRepository detectionRuleRepository;
    private final UserRiskScoreRepository userRiskScoreRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RiskScoreService(
            UserRepository userRepository,
            DetectionRuleRepository detectionRuleRepository,
            UserRiskScoreRepository userRiskScoreRepository
    ) {
        this.userRepository = userRepository;
        this.detectionRuleRepository = detectionRuleRepository;
        this.userRiskScoreRepository = userRiskScoreRepository;
    }

    @Transactional
    public UserRiskScore evaluateSignupRisk(Long userId, String ipAddress, boolean hasContacts) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UserRiskScore riskScore = userRiskScoreRepository.findById(userId)
                .orElseGet(() -> {
                    UserRiskScore rs = new UserRiskScore();
                    rs.setUser(user);
                    rs.setUserId(user.getId());
                    return rs;
                });

        int base = 0;
        List<DetectionRule> rules = detectionRuleRepository.findByEnabledTrue();

        for (DetectionRule rule : rules) {
            if (!"FAKE_ACCOUNT".equalsIgnoreCase(rule.getCategory())) {
                continue;
            }

            try {
                JsonNode params = objectMapper.readTree(rule.getConditionParams());
                switch (rule.getConditionType()) {
                    case "NO_CONTACTS":
                        if (!hasContacts) {
                            base += rule.getRiskDelta();
                        }
                        break;
                    case "IP_PLACEHOLDER":
                        // Placeholder rule type for future IP-based checks
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                logger.warn("Failed to parse condition params for rule {}: {}", rule.getName(), e.getMessage());
            }
        }

        riskScore.setBaseScore(base);
        recomputeTotal(riskScore);
        return userRiskScoreRepository.save(riskScore);
    }

    @Transactional(readOnly = true)
    public int getTotalRiskScore(Long userId) {
        return userRiskScoreRepository.findById(userId)
                .map(UserRiskScore::getTotalScore)
                .orElse(0);
    }

    public boolean isRestricted(Long userId) {
        int score = getTotalRiskScore(userId);
        return score >= 70;
    }

    private void recomputeTotal(UserRiskScore riskScore) {
        int total = safe(riskScore.getBaseScore())
                + safe(riskScore.getBehaviorScore())
                + safe(riskScore.getReportScore())
                + safe(riskScore.getCoordinationScore());
        riskScore.setTotalScore(total);
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }
}


