package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OTP send operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResponse {

    /**
     * Channel used to send OTP (zalo or sms)
     */
    private String channel;

    /**
     * Unique request ID for this OTP
     */
    private String requestId;

    /**
     * Time-to-live in seconds
     */
    private Integer ttlSeconds;

    /**
     * Masked phone number for display
     */
    private String maskedPhone;
}

