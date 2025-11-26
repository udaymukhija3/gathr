package com.gathr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gathr.trust-score")
public class TrustScoreProperties {

    private int baseScore = 100;
    private int maxScore = 200;
    private int minScore = 0;
    private double showUpBonus = 5.0;
    private double noShowPenalty = 10.0;
    private double ratingMultiplier = 10.0;
    private double reportPenalty = 20.0;
    private double activityVolumeBonus = 2.0;
    private int activityVolumeThreshold = 5;
    private int positiveDecayPeriodDays = 90;
    private double positiveDecayFactor = 0.5;
    private int negativeDecayPeriodDays = 90;
    private double negativeDecayFactor = 0.75;
    private int reportDecayPeriodDays = 30;
    private double reportDecayFactor = 0.9;
    private int newAccountDays = 30;
    private double newAccountCeiling = 0.8;

    // Getters and Setters
    public int getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(int baseScore) {
        this.baseScore = baseScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public int getMinScore() {
        return minScore;
    }

    public void setMinScore(int minScore) {
        this.minScore = minScore;
    }

    public double getShowUpBonus() {
        return showUpBonus;
    }

    public void setShowUpBonus(double showUpBonus) {
        this.showUpBonus = showUpBonus;
    }

    public double getNoShowPenalty() {
        return noShowPenalty;
    }

    public void setNoShowPenalty(double noShowPenalty) {
        this.noShowPenalty = noShowPenalty;
    }

    public double getRatingMultiplier() {
        return ratingMultiplier;
    }

    public void setRatingMultiplier(double ratingMultiplier) {
        this.ratingMultiplier = ratingMultiplier;
    }

    public double getReportPenalty() {
        return reportPenalty;
    }

    public void setReportPenalty(double reportPenalty) {
        this.reportPenalty = reportPenalty;
    }

    public double getActivityVolumeBonus() {
        return activityVolumeBonus;
    }

    public void setActivityVolumeBonus(double activityVolumeBonus) {
        this.activityVolumeBonus = activityVolumeBonus;
    }

    public int getActivityVolumeThreshold() {
        return activityVolumeThreshold;
    }

    public void setActivityVolumeThreshold(int activityVolumeThreshold) {
        this.activityVolumeThreshold = activityVolumeThreshold;
    }

    public int getPositiveDecayPeriodDays() {
        return positiveDecayPeriodDays;
    }

    public void setPositiveDecayPeriodDays(int positiveDecayPeriodDays) {
        this.positiveDecayPeriodDays = positiveDecayPeriodDays;
    }

    public double getPositiveDecayFactor() {
        return positiveDecayFactor;
    }

    public void setPositiveDecayFactor(double positiveDecayFactor) {
        this.positiveDecayFactor = positiveDecayFactor;
    }

    public int getNegativeDecayPeriodDays() {
        return negativeDecayPeriodDays;
    }

    public void setNegativeDecayPeriodDays(int negativeDecayPeriodDays) {
        this.negativeDecayPeriodDays = negativeDecayPeriodDays;
    }

    public double getNegativeDecayFactor() {
        return negativeDecayFactor;
    }

    public void setNegativeDecayFactor(double negativeDecayFactor) {
        this.negativeDecayFactor = negativeDecayFactor;
    }

    public int getReportDecayPeriodDays() {
        return reportDecayPeriodDays;
    }

    public void setReportDecayPeriodDays(int reportDecayPeriodDays) {
        this.reportDecayPeriodDays = reportDecayPeriodDays;
    }

    public double getReportDecayFactor() {
        return reportDecayFactor;
    }

    public void setReportDecayFactor(double reportDecayFactor) {
        this.reportDecayFactor = reportDecayFactor;
    }

    public int getNewAccountDays() {
        return newAccountDays;
    }

    public void setNewAccountDays(int newAccountDays) {
        this.newAccountDays = newAccountDays;
    }

    public double getNewAccountCeiling() {
        return newAccountCeiling;
    }

    public void setNewAccountCeiling(double newAccountCeiling) {
        this.newAccountCeiling = newAccountCeiling;
    }
}
