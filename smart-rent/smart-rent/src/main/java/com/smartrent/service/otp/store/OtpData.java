package com.smartrent.service.otp.store;

import com.smartrent.enums.OtpChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Data class representing stored OTP information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Hashed OTP code
     */
    private String hashedCode;

    /**
     * Phone number in E.164 format
     */
    private String phone;

    /**
     * Unique request ID
     */
    private String requestId;

    /**
     * Channel used to send OTP
     */
    private OtpChannel channel;

    /**
     * Number of verification attempts made
     */
    private Integer attempts;

    /**
     * Maximum allowed verification attempts
     */
    private Integer maxAttempts;

    /**
     * Timestamp when OTP was created
     */
    private Instant createdAt;

    /**
     * Timestamp when OTP expires
     */
    private Instant expiresAt;

    /**
     * Whether OTP has been verified
     */
    private Boolean verified;
}

