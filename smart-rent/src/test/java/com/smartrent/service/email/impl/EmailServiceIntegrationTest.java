package com.smartrent.service.email.impl;

import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.connector.model.EmailResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.service.email.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for email service with circuit breaker functionality.
 * This test demonstrates the complete flow including retry logic and error handling.
 */
@SpringBootTest
@ActiveProfiles("test")
class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private BrevoEmailServiceImpl brevoEmailService;

    @Test
    void testEmailServiceWithRetrySuccess() {
        // Given
        EmailInfo emailInfo = EmailInfo.builder()
                .email("test@example.com")
                .name("Test User")
                .build();
        
        EmailRequest emailRequest = EmailRequest.builder()
                .to(List.of(emailInfo))
                .subject("Test Subject")
                .htmlContent("<p>Test content</p>")
                .build();

        EmailResponse emailResponse = EmailResponse.builder()
                .code("200")
                .message("Email sent successfully")
                .build();

        // Mock first call to fail, second to succeed
        FeignException serverError = createFeignException(500, "Internal Server Error");
        when(brevoEmailService.sendEmail(any(EmailRequest.class)))
                .thenThrow(serverError)
                .thenReturn(emailResponse);

        // When
        EmailResponse result = emailService.sendEmail(emailRequest);

        // Then
        assertNotNull(result);
        assertEquals("Email sent successfully", result.getMessage());
        verify(brevoEmailService, times(2)).sendEmail(any(EmailRequest.class));
    }

    @Test
    void testEmailServiceWithNonRetryableError() {
        // Given
        EmailInfo emailInfo = EmailInfo.builder()
                .email("invalid-email")
                .name("Test User")
                .build();
        
        EmailRequest emailRequest = EmailRequest.builder()
                .to(List.of(emailInfo))
                .subject("Test Subject")
                .htmlContent("<p>Test content</p>")
                .build();

        // Mock bad request error (non-retryable)
        FeignException badRequest = createFeignException(400, "Bad Request");
        when(brevoEmailService.sendEmail(any(EmailRequest.class)))
                .thenThrow(badRequest);

        // When & Then
        DomainException exception = assertThrows(DomainException.class, 
                () -> emailService.sendEmail(emailRequest));
        
        // Should only try once for non-retryable errors
        verify(brevoEmailService, times(1)).sendEmail(any(EmailRequest.class));
    }

    @Test
    void testEmailServiceWithAllRetriesExhausted() {
        // Given
        EmailInfo emailInfo = EmailInfo.builder()
                .email("test@example.com")
                .name("Test User")
                .build();
        
        EmailRequest emailRequest = EmailRequest.builder()
                .to(List.of(emailInfo))
                .subject("Test Subject")
                .htmlContent("<p>Test content</p>")
                .build();

        // Mock all calls to fail with server error
        FeignException serverError = createFeignException(503, "Service Unavailable");
        when(brevoEmailService.sendEmail(any(EmailRequest.class)))
                .thenThrow(serverError);

        // When & Then
        DomainException exception = assertThrows(DomainException.class, 
                () -> emailService.sendEmail(emailRequest));
        
        // Should try 3 times for retryable errors
        verify(brevoEmailService, times(3)).sendEmail(any(EmailRequest.class));
    }

    @Test
    void testEmailServiceWithRateLimitingRetry() {
        // Given
        EmailInfo emailInfo = EmailInfo.builder()
                .email("test@example.com")
                .name("Test User")
                .build();
        
        EmailRequest emailRequest = EmailRequest.builder()
                .to(List.of(emailInfo))
                .subject("Test Subject")
                .htmlContent("<p>Test content</p>")
                .build();

        EmailResponse emailResponse = EmailResponse.builder()
                .code("200")
                .message("Email sent successfully")
                .build();

        // Mock rate limiting error then success
        FeignException rateLimitError = createFeignException(429, "Too Many Requests");
        when(brevoEmailService.sendEmail(any(EmailRequest.class)))
                .thenThrow(rateLimitError)
                .thenReturn(emailResponse);

        // When
        EmailResponse result = emailService.sendEmail(emailRequest);

        // Then
        assertNotNull(result);
        assertEquals("Email sent successfully", result.getMessage());
        verify(brevoEmailService, times(2)).sendEmail(any(EmailRequest.class));
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
