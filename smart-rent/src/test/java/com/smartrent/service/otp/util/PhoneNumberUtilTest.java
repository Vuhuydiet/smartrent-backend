package com.smartrent.service.otp.util;

import com.smartrent.infra.exception.OtpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhoneNumberUtil
 */
class PhoneNumberUtilTest {

    private PhoneNumberUtil phoneNumberUtil;

    @BeforeEach
    void setUp() {
        phoneNumberUtil = new PhoneNumberUtil();
    }

    @Test
    void testNormalizeAndValidate_shouldNormalizeVietnameseNumber() {
        String phone = "0912345678";
        String normalized = phoneNumberUtil.normalizeAndValidate(phone);
        
        assertEquals("+84912345678", normalized);
    }

    @Test
    void testNormalizeAndValidate_shouldAcceptE164Format() {
        String phone = "+84912345678";
        String normalized = phoneNumberUtil.normalizeAndValidate(phone);
        
        assertEquals("+84912345678", normalized);
    }

    @Test
    void testNormalizeAndValidate_shouldAccept84Prefix() {
        String phone = "84912345678";
        String normalized = phoneNumberUtil.normalizeAndValidate(phone);
        
        assertEquals("+84912345678", normalized);
    }

    @Test
    void testNormalizeAndValidate_shouldThrowExceptionForNullPhone() {
        assertThrows(OtpException.class, () -> {
            phoneNumberUtil.normalizeAndValidate(null);
        });
    }

    @Test
    void testNormalizeAndValidate_shouldThrowExceptionForEmptyPhone() {
        assertThrows(OtpException.class, () -> {
            phoneNumberUtil.normalizeAndValidate("");
        });
    }

    @Test
    void testNormalizeAndValidate_shouldThrowExceptionForInvalidPhone() {
        assertThrows(OtpException.class, () -> {
            phoneNumberUtil.normalizeAndValidate("123");
        });
    }

    @Test
    void testNormalizeAndValidate_shouldThrowExceptionForNonVietnameseNumber() {
        assertThrows(OtpException.class, () -> {
            phoneNumberUtil.normalizeAndValidate("+1234567890"); // US number
        });
    }

    @Test
    void testMaskPhoneNumber_shouldMaskMiddleDigits() {
        String phone = "+84912345678";
        String masked = phoneNumberUtil.maskPhoneNumber(phone);
        
        assertTrue(masked.contains("***"));
        assertTrue(masked.startsWith("+8491"));
        assertTrue(masked.endsWith("5678"));
    }

    @Test
    void testMaskPhoneNumber_shouldHandleShortNumbers() {
        String phone = "123";
        String masked = phoneNumberUtil.maskPhoneNumber(phone);
        
        assertEquals(phone, masked);
    }

    @Test
    void testMaskPhoneNumber_shouldHandleNullPhone() {
        String masked = phoneNumberUtil.maskPhoneNumber(null);
        
        assertNull(masked);
    }

    @Test
    void testIsVietnamMobileNumber_shouldReturnTrueForMobileNumber() {
        String phone = "+84912345678";
        boolean isMobile = phoneNumberUtil.isVietnamMobileNumber(phone);
        
        assertTrue(isMobile);
    }

    @Test
    void testIsVietnamMobileNumber_shouldReturnFalseForInvalidNumber() {
        String phone = "invalid";
        boolean isMobile = phoneNumberUtil.isVietnamMobileNumber(phone);
        
        assertFalse(isMobile);
    }
}

