package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OTP verification operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyResponse {

    /**
     * Whether the OTP was verified successfully
     */
    private Boolean verified;

    /**
     * Message describing the result
     */
    private String message;

    /**
     * Remaining verification attempts (if verification failed)
     */
    private Integer remainingAttempts;
}

