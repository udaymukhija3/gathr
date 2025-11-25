package com.gathr.service.impl;

import com.gathr.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of OTP service for development/testing
 * Always uses OTP "123456" for any phone number
 */
@Service
@ConditionalOnProperty(name = "otp.provider", havingValue = "mock", matchIfMissing = true)
public class MockOtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(MockOtpServiceImpl.class);
    private static final String MOCK_OTP = "123456";

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    // In-memory storage for OTPs (use Redis in production)
    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    @Override
    public String sendOtp(String phone) {
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        otpStorage.put(phone, new OtpEntry(MOCK_OTP, expiryTime));

        logger.info("Mock OTP sent to {}: {} (expires at {})", phone, MOCK_OTP, expiryTime);
        return MOCK_OTP;
    }

    @Override
    public boolean verifyOtp(String phone, String otp) {
        OtpEntry entry = otpStorage.get(phone);

        if (entry == null) {
            logger.warn("No OTP found for phone: {}", phone);
            return false;
        }

        if (LocalDateTime.now().isAfter(entry.expiryTime())) {
            logger.warn("OTP expired for phone: {}", phone);
            otpStorage.remove(phone);
            return false;
        }

        boolean isValid = entry.otp().equals(otp);
        if (isValid) {
            logger.info("OTP verified successfully for phone: {}", phone);
            otpStorage.remove(phone); // Remove OTP after successful verification
        } else {
            logger.warn("Invalid OTP for phone: {}", phone);
        }

        return isValid;
    }

    @Override
    public void clearOtp(String phone) {
        otpStorage.remove(phone);
        logger.info("OTP cleared for phone: {}", phone);
    }

    private record OtpEntry(String otp, LocalDateTime expiryTime) {}
}
