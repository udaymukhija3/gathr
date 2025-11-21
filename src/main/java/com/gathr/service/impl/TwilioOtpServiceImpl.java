package com.gathr.service.impl;

import com.gathr.service.OtpService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Twilio implementation of OTP service
 * To use this implementation:
 * 1. Add Twilio SDK dependency to pom.xml
 * 2. Set otp.provider=twilio in application.properties
 * 3. Configure Twilio credentials in application.properties
 */
@Service
@ConditionalOnProperty(name = "otp.provider", havingValue = "twilio")
public class TwilioOtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioOtpServiceImpl.class);
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String fromPhoneNumber;

    // In-memory storage for OTPs (use Redis in production)
    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (!accountSid.isEmpty() && !authToken.isEmpty()) {
            Twilio.init(accountSid, authToken);
            logger.info("Twilio initialized with phone number: {}", fromPhoneNumber);
        } else {
            logger.error("Twilio credentials not configured! Set twilio.account-sid and twilio.auth-token");
        }
    }

    @Override
    public String sendOtp(String phone) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", RANDOM.nextInt(1000000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        otpStorage.put(phone, new OtpEntry(otp, expiryTime));

        try {
            Message message = Message.creator(
                    new PhoneNumber(phone),
                    new PhoneNumber(fromPhoneNumber),
                    "Your Gathr verification code is: " + otp + "\n\nThis code will expire in " + OTP_EXPIRY_MINUTES + " minutes."
            ).create();

            logger.info("OTP sent via Twilio to {}: {}", phone, message.getSid());
        } catch (Exception e) {
            logger.error("Failed to send OTP via Twilio to {}: {}", phone, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP", e);
        }

        return otp;
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
            otpStorage.remove(phone);
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
