package com.smartrent.service.otp.provider;

import com.smartrent.config.otp.TwilioProperties;
import com.smartrent.enums.OtpChannel;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Twilio SMS provider for OTP delivery (fallback channel)
 * 
 * IMPORTANT: SMS sender registration requirements in Vietnam:
 * 1. Brand name registration with Vietnamese telecom operators
 * 2. SMS content template approval
 * 3. Sender ID registration (typically takes 2-4 weeks)
 * 4. Compliance with Vietnam's telecommunications regulations
 * 
 * For production in Vietnam, consider using local SMS providers like:
 * - VIETGUYS, VNPT, FPT, Viettel, etc.
 * 
 * Twilio can be used for international SMS or as a development fallback.
 * Check latest Twilio SDK version before production deployment.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "otp.providers.twilio.enabled", havingValue = "true")
public class TwilioVerifyProvider implements OtpProvider {

    private final TwilioProperties twilioProperties;

    public TwilioVerifyProvider(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
    }

    @PostConstruct
    public void init() {
        if (isAvailable()) {
            Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
            log.info("Twilio SMS provider initialized");
        } else {
            log.warn("Twilio SMS provider not configured properly");
        }
    }

    @Override
    public OtpChannel getChannel() {
        return OtpChannel.SMS;
    }

    @Override
    public OtpProviderResult sendOtp(String phone, String otpCode, Map<String, Object> context) {
        log.info("Sending OTP via Twilio SMS to phone: {}", maskPhone(phone));

        try {
            // Build SMS message
            String messageBody = buildSmsMessage(otpCode, context);
            
            // Send SMS using Twilio
            Message message = Message.creator(
                new PhoneNumber(phone),
                new PhoneNumber(twilioProperties.getFromNumber()),
                messageBody
            ).create();

            log.info("Twilio SMS sent successfully, SID: {}, status: {}", 
                message.getSid(), message.getStatus());
            
            return OtpProviderResult.success(message.getSid());

        } catch (ApiException e) {
            log.error("Twilio API error: code={}, message={}", e.getCode(), e.getMessage());
            return handleTwilioError(e);
        } catch (Exception e) {
            log.error("Failed to send OTP via Twilio SMS", e);
            return OtpProviderResult.failure("TWILIO_ERROR", e.getMessage(), true);
        }
    }

    @Override
    public boolean isAvailable() {
        return twilioProperties.getAccountSid() != null && 
               !twilioProperties.getAccountSid().isEmpty() &&
               twilioProperties.getAuthToken() != null &&
               !twilioProperties.getAuthToken().isEmpty() &&
               twilioProperties.getFromNumber() != null &&
               !twilioProperties.getFromNumber().isEmpty();
    }

    @Override
    public String getProviderName() {
        return "Twilio SMS";
    }

    /**
     * Build SMS message content
     * Keep message concise to minimize SMS costs
     */
    private String buildSmsMessage(String otpCode, Map<String, Object> context) {
        // Use custom template from context if provided, otherwise use configured template
        String template = twilioProperties.getMessageTemplate();

        if (context != null && context.containsKey("messageTemplate")) {
            template = (String) context.get("messageTemplate");
        }

        return String.format(template, otpCode);
    }

    /**
     * Handle Twilio API errors
     * Common Twilio error codes:
     * 21211: Invalid phone number
     * 21408: Permission denied (phone number not verified in trial)
     * 21610: Unsubscribed recipient
     * 30003: Unreachable destination
     * 30005: Unknown destination
     */
    private OtpProviderResult handleTwilioError(ApiException e) {
        int errorCode = e.getCode();
        String errorMessage = e.getMessage();
        
        // Determine if error is retryable
        boolean retryable = switch (errorCode) {
            case 21211, 21408, 21610, 30003, 30005 -> false; // Not retryable
            default -> true; // Retryable (network issues, rate limits, etc.)
        };
        
        return OtpProviderResult.failure(
            "TWILIO_" + errorCode,
            errorMessage,
            retryable
        );
    }

    /**
     * Mask phone number for logging
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 5) + "***" + phone.substring(phone.length() - 3);
    }
}

