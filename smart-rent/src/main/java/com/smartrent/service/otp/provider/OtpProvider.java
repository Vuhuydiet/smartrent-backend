package com.smartrent.service.otp.provider;

import com.smartrent.enums.OtpChannel;

import java.util.Map;

/**
 * Interface for OTP delivery providers
 * Implementations should handle sending OTP via different channels (Zalo, SMS, etc.)
 */
public interface OtpProvider {

    /**
     * Get the channel this provider supports
     *
     * @return OTP channel
     */
    OtpChannel getChannel();

    /**
     * Send OTP to the specified phone number
     *
     * @param phone Phone number in E.164 format
     * @param otpCode OTP code to send
     * @param context Additional context data (e.g., template parameters)
     * @return Result of the send operation
     */
    OtpProviderResult sendOtp(String phone, String otpCode, Map<String, Object> context);

    /**
     * Check if this provider is available/enabled
     *
     * @return true if provider is available
     */
    boolean isAvailable();

    /**
     * Get provider name for logging
     *
     * @return Provider name
     */
    String getProviderName();
}

