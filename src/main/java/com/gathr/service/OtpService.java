package com.gathr.service;

/**
 * Interface for OTP (One-Time Password) service
 * Implementations can use various SMS providers like Twilio, AWS SNS, etc.
 */
public interface OtpService {

    /**
     * Generate and send OTP to the given phone number
     *
     * @param phone the phone number to send OTP to
     * @return the generated OTP (for testing/mock implementations only)
     */
    String sendOtp(String phone);

    /**
     * Verify if the provided OTP is valid for the given phone number
     *
     * @param phone the phone number
     * @param otp the OTP to verify
     * @return true if OTP is valid, false otherwise
     */
    boolean verifyOtp(String phone, String otp);

    /**
     * Invalidate/clear OTP for the given phone number
     *
     * @param phone the phone number
     */
    void clearOtp(String phone);
}
