package com.gathr.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
public class TwilioOtpService implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioOtpService.class);
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @Value("${twilio.enabled:false}")
    private boolean isEnabled;

    @Override
    public String sendOtp(String phone) {
        String otp = String.format("%06d", random.nextInt(1000000));
        otpStorage.put(phone, otp);

        if (isEnabled) {
            try {
                Twilio.init(accountSid, authToken);
                Message.creator(
                        new PhoneNumber(phone),
                        new PhoneNumber(fromPhoneNumber),
                        "Your Gathr verification code is: " + otp).create();
                logger.info("Sent OTP via Twilio to {}", phone);
            } catch (Exception e) {
                logger.error("Failed to send OTP via Twilio", e);
                // Fallback or rethrow depending on requirements. For now, log error.
            }
        } else {
            logger.info("Twilio disabled. Mock OTP for {}: {}", phone, otp);
        }
        return otp;
    }

    @Override
    public boolean verifyOtp(String phone, String otp) {
        String storedOtp = otpStorage.get(phone);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStorage.remove(phone);
            return true;
        }
        return false;
    }

    @Override
    public void clearOtp(String phone) {
        otpStorage.remove(phone);
    }
}
