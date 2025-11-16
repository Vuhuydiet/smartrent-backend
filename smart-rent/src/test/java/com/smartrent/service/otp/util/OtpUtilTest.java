package com.smartrent.service.otp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OtpUtil
 */
class OtpUtilTest {

    private OtpUtil otpUtil;

    @BeforeEach
    void setUp() {
        otpUtil = new OtpUtil();
    }

    @Test
    void testGenerateOtpCode_shouldReturn6Digits() {
        String otp = otpUtil.generateOtpCode();
        
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"), "OTP should contain only digits");
    }

    @Test
    void testGenerateOtpCode_shouldBeUnique() {
        String otp1 = otpUtil.generateOtpCode();
        String otp2 = otpUtil.generateOtpCode();
        
        // While not guaranteed, it's extremely unlikely to generate the same OTP twice
        assertNotEquals(otp1, otp2);
    }

    @Test
    void testHashOtpCode_shouldReturnNonEmptyHash() {
        String code = "123456";
        String hash = otpUtil.hashOtpCode(code);
        
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertNotEquals(code, hash, "Hash should be different from plain code");
    }

    @Test
    void testHashOtpCode_shouldProduceDifferentHashesForSameCode() {
        String code = "123456";
        String hash1 = otpUtil.hashOtpCode(code);
        String hash2 = otpUtil.hashOtpCode(code);
        
        // BCrypt produces different hashes for the same input due to salt
        assertNotEquals(hash1, hash2);
    }

    @Test
    void testVerifyOtpCode_shouldReturnTrueForMatchingCode() {
        String code = "123456";
        String hash = otpUtil.hashOtpCode(code);
        
        boolean result = otpUtil.verifyOtpCode(code, hash);
        
        assertTrue(result);
    }

    @Test
    void testVerifyOtpCode_shouldReturnFalseForNonMatchingCode() {
        String code = "123456";
        String wrongCode = "654321";
        String hash = otpUtil.hashOtpCode(code);
        
        boolean result = otpUtil.verifyOtpCode(wrongCode, hash);
        
        assertFalse(result);
    }

    @Test
    void testVerifyOtpCode_shouldReturnFalseForInvalidHash() {
        String code = "123456";
        String invalidHash = "invalid_hash";
        
        boolean result = otpUtil.verifyOtpCode(code, invalidHash);
        
        assertFalse(result);
    }
}

