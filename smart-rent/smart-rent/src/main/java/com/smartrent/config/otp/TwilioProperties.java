package com.smartrent.config.otp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Twilio SMS provider
 * 
 * Required environment variables:
 * - TWILIO_ACCOUNT_SID: Twilio account SID
 * - TWILIO_AUTH_TOKEN: Twilio auth token
 * - TWILIO_FROM_NUMBER: Twilio phone number to send from
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "otp.providers.twilio")
public class TwilioProperties {

    /**
     * Enable/disable Twilio provider
     */
    private boolean enabled = false;

    /**
     * Twilio account SID
     */
    private String accountSid;

    /**
     * Twilio auth token
     */
    private String authToken;

    /**
     * Phone number to send SMS from (E.164 format)
     */
    private String fromNumber;

    /**
     * SMS message template
     * Use %s as placeholder for OTP code
     */
    private String messageTemplate = "SmartRent - Your verification code is %s. Valid for 5 minutes. Do not share this code.";
}

