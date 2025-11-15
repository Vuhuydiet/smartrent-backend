package com.smartrent.service.otp.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of OTP provider send operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpProviderResult {

    /**
     * Whether the send was successful
     */
    private boolean success;

    /**
     * Provider-specific message ID or transaction ID
     */
    private String messageId;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * Error code if failed
     */
    private String errorCode;

    /**
     * Whether the error is retryable
     */
    private boolean retryable;

    public static OtpProviderResult success(String messageId) {
        return OtpProviderResult.builder()
            .success(true)
            .messageId(messageId)
            .build();
    }

    public static OtpProviderResult failure(String errorCode, String errorMessage, boolean retryable) {
        return OtpProviderResult.builder()
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .retryable(retryable)
            .build();
    }
}

