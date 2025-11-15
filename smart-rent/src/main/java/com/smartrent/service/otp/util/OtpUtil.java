package com.smartrent.service.otp.util;

import com.smartrent.infra.exception.OtpException;
import com.smartrent.infra.exception.model.DomainCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility class for OTP generation and hashing operations
 */
@Slf4j
@Component
public class OtpUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(10);

    /**
     * Generate a cryptographically secure 6-digit OTP code
     *
     * @return 6-digit OTP code as string
     */
    public String generateOtpCode() {
        try {
            // Generate a random number between 0 and 999999
            int otp = SECURE_RANDOM.nextInt(1000000);
            // Format to 6 digits with leading zeros
            return String.format("%06d", otp);
        } catch (Exception e) {
            log.error("Failed to generate OTP code", e);
            throw new OtpException(DomainCode.OTP_GENERATION_FAILED);
        }
    }

    /**
     * Hash an OTP code using BCrypt
     *
     * @param code Plain OTP code
     * @return Hashed OTP code
     */
    public String hashOtpCode(String code) {
        try {
            return PASSWORD_ENCODER.encode(code);
        } catch (Exception e) {
            log.error("Failed to hash OTP code", e);
            throw new OtpException(DomainCode.OTP_GENERATION_FAILED);
        }
    }

    /**
     * Verify an OTP code against its hash
     *
     * @param plainCode Plain OTP code to verify
     * @param hashedCode Hashed OTP code to compare against
     * @return true if codes match, false otherwise
     */
    public boolean verifyOtpCode(String plainCode, String hashedCode) {
        try {
            return PASSWORD_ENCODER.matches(plainCode, hashedCode);
        } catch (Exception e) {
            log.error("Failed to verify OTP code", e);
            return false;
        }
    }
}

