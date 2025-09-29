package com.smartrent.service.email.impl;

import com.smartrent.config.retry.email.EmailRetryProperties;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.connector.model.EmailResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for ResilientEmailServiceImpl to verify circuit breaker and retry functionality.
 */
@ExtendWith(MockitoExtension.class)
class ResilientEmailServiceImplTest {

    @Mock
    private BrevoEmailServiceImpl brevoEmailService;

    @Mock
    private EmailRetryProperties retryProperties;

    @InjectMocks
    private ResilientEmailServiceImpl resilientEmailService;

    private EmailRequest emailRequest;
    private EmailResponse emailResponse;

    @BeforeEach
    void setUp() {
        // Create test email request
        EmailInfo emailInfo = EmailInfo.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        emailRequest = EmailRequest.builder()
                .to(List.of(emailInfo))
                .subject("Test Subject")
                .htmlContent("<p>Test content</p>")
                .build();

        // Create test email response
        emailResponse = EmailResponse.builder()
                .code("200")
                .message("Email sent successfully")
                .build();

        // Setup default retry properties with lenient stubbing
        lenient().when(retryProperties.getMaxAttempts()).thenReturn(3);
        lenient().when(retryProperties.getBaseWaitDuration()).thenReturn(100L);
        lenient().when(retryProperties.getExponentialMultiplier()).thenReturn(2.0);
        lenient().when(retryProperties.getJitterPercentage()).thenReturn(0.1);
        lenient().when(retryProperties.getMinWaitDuration()).thenReturn(50L);
    }

    @Test
    void sendEmail_Success_ShouldReturnResponse() {
        // Given
        when(brevoEmailService.sendEmail(emailRequest)).thenReturn(emailResponse);

        // When
        EmailResponse result = resilientEmailService.sendEmail(emailRequest);

        // Then
        assertNotNull(result);
        assertEquals("Email sent successfully", result.getMessage());
        verify(brevoEmailService, times(1)).sendEmail(emailRequest);
    }

    @Test
    void sendEmail_TransientFailureThenSuccess_ShouldRetryAndSucceed() {
        // Given
        FeignException serverError = createFeignException(500, "Internal Server Error");
        when(brevoEmailService.sendEmail(emailRequest))
                .thenThrow(serverError)
                .thenReturn(emailResponse);

        // When
        EmailResponse result = resilientEmailService.sendEmail(emailRequest);

        // Then
        assertNotNull(result);
        assertEquals("Email sent successfully", result.getMessage());
        verify(brevoEmailService, times(2)).sendEmail(emailRequest);
    }

    @Test
    void sendEmail_PermanentFailure_ShouldNotRetry() {
        // Given
        FeignException badRequest = createFeignException(400, "Bad Request");
        when(brevoEmailService.sendEmail(emailRequest)).thenThrow(badRequest);

        // When & Then
        DomainException exception = assertThrows(DomainException.class, 
                () -> resilientEmailService.sendEmail(emailRequest));
        
        assertEquals(DomainCode.CANNOT_SEND_EMAIL, exception.getDomainCode());
        verify(brevoEmailService, times(1)).sendEmail(emailRequest);
    }

    @Test
    void sendEmail_RateLimitingError_ShouldRetry() {
        // Given
        FeignException rateLimitError = createFeignException(429, "Too Many Requests");
        when(brevoEmailService.sendEmail(emailRequest))
                .thenThrow(rateLimitError)
                .thenReturn(emailResponse);

        // When
        EmailResponse result = resilientEmailService.sendEmail(emailRequest);

        // Then
        assertNotNull(result);
        assertEquals("Email sent successfully", result.getMessage());
        verify(brevoEmailService, times(2)).sendEmail(emailRequest);
    }

    @Test
    void sendEmail_AllRetriesExhausted_ShouldThrowDomainException() {
        // Given
        FeignException serverError = createFeignException(500, "Internal Server Error");
        when(brevoEmailService.sendEmail(emailRequest)).thenThrow(serverError);

        // When & Then
        DomainException exception = assertThrows(DomainException.class, 
                () -> resilientEmailService.sendEmail(emailRequest));
        
        assertEquals(DomainCode.CANNOT_SEND_EMAIL, exception.getDomainCode());
        // Should retry 3 times (initial + 2 retries)
        verify(brevoEmailService, times(3)).sendEmail(emailRequest);
    }

    @Test
    void sendEmail_UnexpectedException_ShouldThrowDomainException() {
        // Given
        RuntimeException unexpectedException = new RuntimeException("Unexpected error");
        when(brevoEmailService.sendEmail(emailRequest)).thenThrow(unexpectedException);

        // When & Then
        DomainException exception = assertThrows(DomainException.class, 
                () -> resilientEmailService.sendEmail(emailRequest));
        
        assertEquals(DomainCode.CANNOT_SEND_EMAIL, exception.getDomainCode());
        verify(brevoEmailService, times(1)).sendEmail(emailRequest);
    }

    @Test
    void isRetryableError_ServerError_ShouldReturnTrue() {
        // Given
        FeignException serverError = createFeignException(500, "Internal Server Error");

        // When
        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(
                resilientEmailService, "isRetryableError", serverError);

        // Then
        assertTrue(result);
    }

    @Test
    void isRetryableError_ClientError_ShouldReturnFalse() {
        // Given
        FeignException clientError = createFeignException(400, "Bad Request");

        // When
        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(
                resilientEmailService, "isRetryableError", clientError);

        // Then
        assertFalse(result);
    }

    @Test
    void isRetryableError_RateLimiting_ShouldReturnTrue() {
        // Given
        FeignException rateLimitError = createFeignException(429, "Too Many Requests");

        // When
        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(
                resilientEmailService, "isRetryableError", rateLimitError);

        // Then
        assertTrue(result);
    }

    @Test
    void sendEmailFallback_ShouldThrowDomainException() {
        // Given
        Exception cause = new RuntimeException("Circuit breaker open");

        // When & Then
        DomainException exception = assertThrows(DomainException.class, 
                () -> resilientEmailService.sendEmailFallback(emailRequest, cause));
        
        assertEquals(DomainCode.CANNOT_SEND_EMAIL, exception.getDomainCode());
    }

    /**
     * Helper method to create FeignException instances for testing
     */
    private FeignException createFeignException(int status, String message) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://test.com/api",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        
        return new FeignException.FeignServerException(status, message, request, null, null);
    }
}
