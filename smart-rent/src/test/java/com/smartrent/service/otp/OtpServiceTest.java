package com.smartrent.service.otp;

import com.smartrent.config.otp.OtpProperties;
import com.smartrent.dto.request.OtpSendRequest;
import com.smartrent.dto.request.OtpVerifyRequest;
import com.smartrent.dto.response.OtpSendResponse;
import com.smartrent.dto.response.OtpVerifyResponse;
import com.smartrent.enums.OtpChannel;
import com.smartrent.infra.exception.OtpNotFoundException;
import com.smartrent.service.otp.provider.OtpProvider;
import com.smartrent.service.otp.provider.OtpProviderResult;
import com.smartrent.service.otp.store.OtpData;
import com.smartrent.service.otp.store.OtpStore;
import com.smartrent.service.otp.util.OtpUtil;
import com.smartrent.service.otp.util.PhoneNumberUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OtpService
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpStore otpStore;

    @Mock
    private OtpUtil otpUtil;

    @Mock
    private PhoneNumberUtil phoneNumberUtil;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private OtpProvider zaloProvider;

    @Mock
    private OtpProvider smsProvider;

    private OtpService otpService;
    private OtpProperties otpProperties;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        otpProperties = new OtpProperties();
        otpProperties.setCodeLength(6);
        otpProperties.setTtlSeconds(300);
        otpProperties.setMaxVerificationAttempts(5);

        meterRegistry = new SimpleMeterRegistry();

        when(zaloProvider.getChannel()).thenReturn(OtpChannel.ZALO);
        when(zaloProvider.getProviderName()).thenReturn("Zalo ZNS");
        when(smsProvider.getChannel()).thenReturn(OtpChannel.SMS);
        when(smsProvider.getProviderName()).thenReturn("Twilio SMS");

        otpService = new OtpService(
            otpProperties,
            otpStore,
            otpUtil,
            phoneNumberUtil,
            rateLimitService,
            Arrays.asList(zaloProvider, smsProvider),
            meterRegistry
        );
    }

    @Test
    void testSendOtp_shouldSendViaZaloSuccessfully() {
        // Arrange
        OtpSendRequest request = OtpSendRequest.builder()
            .phone("0912345678")
            .build();

        when(phoneNumberUtil.normalizeAndValidate(anyString())).thenReturn("+84912345678");
        when(phoneNumberUtil.maskPhoneNumber(anyString())).thenReturn("+8491***5678");
        when(otpUtil.generateOtpCode()).thenReturn("123456");
        when(otpUtil.hashOtpCode(anyString())).thenReturn("hashed_code");
        when(zaloProvider.isAvailable()).thenReturn(true);
        when(zaloProvider.sendOtp(anyString(), anyString(), any())).thenReturn(
            OtpProviderResult.success("msg-123")
        );
        when(otpStore.store(any(OtpData.class), anyInt())).thenReturn(true);

        // Act
        OtpSendResponse response = otpService.sendOtp(request, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertEquals("zalo", response.getChannel());
        assertNotNull(response.getRequestId());
        assertEquals(300, response.getTtlSeconds());

        verify(rateLimitService).checkPhoneRateLimit("+84912345678");
        verify(rateLimitService).checkIpRateLimit("127.0.0.1");
        verify(zaloProvider).sendOtp(eq("+84912345678"), eq("123456"), any());
        verify(otpStore).store(any(OtpData.class), eq(300));
    }

    @Test
    void testSendOtp_shouldFallbackToSmsWhenZaloFails() {
        // Arrange
        OtpSendRequest request = OtpSendRequest.builder()
            .phone("0912345678")
            .build();

        when(phoneNumberUtil.normalizeAndValidate(anyString())).thenReturn("+84912345678");
        when(phoneNumberUtil.maskPhoneNumber(anyString())).thenReturn("+8491***5678");
        when(otpUtil.generateOtpCode()).thenReturn("123456");
        when(otpUtil.hashOtpCode(anyString())).thenReturn("hashed_code");
        when(zaloProvider.isAvailable()).thenReturn(true);
        when(zaloProvider.sendOtp(anyString(), anyString(), any())).thenReturn(
            OtpProviderResult.failure("ZALO_ERROR", "Zalo failed", false)
        );
        when(smsProvider.isAvailable()).thenReturn(true);
        when(smsProvider.sendOtp(anyString(), anyString(), any())).thenReturn(
            OtpProviderResult.success("sms-123")
        );
        when(otpStore.store(any(OtpData.class), anyInt())).thenReturn(true);

        // Act
        OtpSendResponse response = otpService.sendOtp(request, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertEquals("sms", response.getChannel());

        verify(zaloProvider).sendOtp(anyString(), anyString(), any());
        verify(smsProvider).sendOtp(anyString(), anyString(), any());
    }

    @Test
    void testVerifyOtp_shouldVerifySuccessfully() {
        // Arrange
        OtpVerifyRequest request = OtpVerifyRequest.builder()
            .phone("0912345678")
            .code("123456")
            .requestId("test-request-id")
            .build();

        OtpData otpData = OtpData.builder()
            .hashedCode("hashed_code")
            .phone("+84912345678")
            .requestId("test-request-id")
            .channel(OtpChannel.ZALO)
            .attempts(0)
            .maxAttempts(5)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .verified(false)
            .build();

        when(phoneNumberUtil.normalizeAndValidate(anyString())).thenReturn("+84912345678");
        when(phoneNumberUtil.maskPhoneNumber(anyString())).thenReturn("+8491***5678");
        when(otpStore.retrieve(anyString(), anyString())).thenReturn(Optional.of(otpData));
        when(otpUtil.verifyOtpCode(anyString(), anyString())).thenReturn(true);

        // Act
        OtpVerifyResponse response = otpService.verifyOtp(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getVerified());
        assertEquals("OTP verified successfully", response.getMessage());

        verify(otpStore).delete("+84912345678", "test-request-id");
    }

    @Test
    void testVerifyOtp_shouldFailForInvalidCode() {
        // Arrange
        OtpVerifyRequest request = OtpVerifyRequest.builder()
            .phone("0912345678")
            .code("654321")
            .requestId("test-request-id")
            .build();

        OtpData otpData = OtpData.builder()
            .hashedCode("hashed_code")
            .phone("+84912345678")
            .requestId("test-request-id")
            .channel(OtpChannel.ZALO)
            .attempts(0)
            .maxAttempts(5)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .verified(false)
            .build();

        when(phoneNumberUtil.normalizeAndValidate(anyString())).thenReturn("+84912345678");
        when(phoneNumberUtil.maskPhoneNumber(anyString())).thenReturn("+8491***5678");
        when(otpStore.retrieve(anyString(), anyString())).thenReturn(Optional.of(otpData));
        when(otpUtil.verifyOtpCode(anyString(), anyString())).thenReturn(false);

        // Act
        OtpVerifyResponse response = otpService.verifyOtp(request);

        // Assert
        assertNotNull(response);
        assertFalse(response.getVerified());
        assertEquals("Invalid OTP code", response.getMessage());
        assertEquals(4, response.getRemainingAttempts());

        verify(otpStore).update(any(OtpData.class), anyInt());
    }

    @Test
    void testVerifyOtp_shouldThrowExceptionForNonExistentOtp() {
        // Arrange
        OtpVerifyRequest request = OtpVerifyRequest.builder()
            .phone("0912345678")
            .code("123456")
            .requestId("non-existent")
            .build();

        when(phoneNumberUtil.normalizeAndValidate(anyString())).thenReturn("+84912345678");
        when(phoneNumberUtil.maskPhoneNumber(anyString())).thenReturn("+8491***5678");
        when(otpStore.retrieve(anyString(), anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OtpNotFoundException.class, () -> {
            otpService.verifyOtp(request);
        });
    }
}

