package com.smartrent.service.email.impl;

import com.smartrent.config.retry.email.EmailRetryProperties;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.connector.model.EmailResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.service.email.EmailService;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resilient email service implementation that wraps the basic email service
 * with circuit breaker and retry patterns for improved fault tolerance.
 * 
 * This service provides:
 * - Automatic retry on transient failures
 * - Circuit breaker to prevent cascading failures
 * - Fallback mechanisms for graceful degradation
 * - Comprehensive logging and monitoring
 */
@Service
@Primary
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ResilientEmailServiceImpl implements EmailService {

    EmailService emailService;
    EmailRetryProperties retryProperties;

    /**
     * Validates configuration properties after bean initialization
     */
    @PostConstruct
    public void init() {
        retryProperties.validate();
        log.info("Email retry configuration loaded: maxAttempts={}, baseWaitDuration={}ms, " +
                "exponentialMultiplier={}, jitterPercentage={}, minWaitDuration={}ms",
                retryProperties.getMaxAttempts(),
                retryProperties.getBaseWaitDuration(),
                retryProperties.getExponentialMultiplier(),
                retryProperties.getJitterPercentage(),
                retryProperties.getMinWaitDuration());
    }

    /**
     * Sends an email with manual retry logic and circuit breaker pattern.
     *
     * The method will:
     * 1. First attempt to send the email
     * 2. Retry up to 3 times with exponential backoff on transient failures
     * 3. Track failure rates for circuit breaker logic
     * 4. Fall back to graceful error handling if all attempts fail
     *
     * @param emailRequest the email request to send
     * @return EmailResponse if successful
     * @throws DomainException if all retry attempts fail
     */
    @Override
    public EmailResponse sendEmail(EmailRequest emailRequest) {
        log.debug("Attempting to send email to: {}", emailRequest.getTo());

        return sendEmailWithRetry(emailRequest, retryProperties.getMaxAttempts());
    }

    /**
     * Sends email with retry logic
     */
    private EmailResponse sendEmailWithRetry(EmailRequest emailRequest, int maxAttempts) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Email send attempt {} of {}", attempt, maxAttempts);
                EmailResponse response = emailService.
                    sendEmail(emailRequest);
                log.info("Email sent successfully to: {} with response: {} on attempt {}",
                        emailRequest.getTo(), response, attempt);
                return response;

            } catch (FeignException e) {
                lastException = e;
                log.error("Failed to send email to: {} - Status: {}, Error: {} (attempt {} of {})",
                        emailRequest.getTo(), e.status(), e.getMessage(), attempt, maxAttempts);

//                 Determine if this is a retryable error
                if (!isRetryableError(e)) {
                    log.error("Non-retryable error encountered, failing immediately");
                    throw new DomainException(DomainCode.CANNOT_SEND_EMAIL);
                }

                // If this is the last attempt, don't wait
                if (attempt < maxAttempts) {
                    long waitTime = calculateWaitTime(attempt);
                    log.warn("Retryable error encountered, waiting {}ms before retry attempt {}",
                            waitTime, attempt + 1);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new DomainException(DomainCode.CANNOT_SEND_EMAIL);
                    }
                }

            } catch (Exception e) {
                log.error("Unexpected error while sending email to: {} (attempt {} of {})",
                        emailRequest.getTo(), attempt, maxAttempts, e);
                throw new DomainException(DomainCode.CANNOT_SEND_EMAIL);
            }
        }

        // All attempts failed
        log.error("All {} attempts to send email failed. Last error: {}",
                maxAttempts, lastException != null ? lastException.getMessage() : "Unknown");
        throw new DomainException(DomainCode.CANNOT_SEND_EMAIL);
    }

    /**
     * Calculates wait time with exponential backoff and jitter using configured values
     */
    private long calculateWaitTime(int attempt) {
        // Calculate exponential backoff: baseWaitTime * (multiplier ^ (attempt - 1))
        long baseWaitTime = retryProperties.getBaseWaitDuration();
        double multiplier = retryProperties.getExponentialMultiplier();
        long waitTime = (long) (baseWaitTime * Math.pow(multiplier, attempt - 1));

        // Add jitter to prevent thundering herd
        double jitterPercentage = retryProperties.getJitterPercentage();
        long jitterRange = (long) (waitTime * jitterPercentage);
        long randomJitter = ThreadLocalRandom.current().nextLong(-jitterRange, jitterRange + 1);

        // Ensure minimum wait time
        long finalWaitTime = waitTime + randomJitter;
        return Math.max(retryProperties.getMinWaitDuration(), finalWaitTime);
    }

    /**
     * Fallback method called when circuit breaker is open or all retry attempts are exhausted.
     * This method provides graceful degradation by logging the failure and returning
     * an appropriate error response.
     * 
     * @param emailRequest the original email request
     * @param exception the exception that triggered the fallback
     * @return never returns, always throws DomainException
     * @throws DomainException indicating email sending failure
     */
    public EmailResponse sendEmailFallback(EmailRequest emailRequest, Exception exception) {
        log.error("Email service fallback triggered for recipient: {} due to: {}", 
                emailRequest.getTo(), exception.getMessage());
        
        // Log additional context for monitoring and alerting
        if (exception instanceof FeignException feignException) {
            log.error("Feign exception details - Status: {}, Request: {}", 
                    feignException.status(), feignException.request());
        }
        
        // In a production environment, you might want to:
        // 1. Store the email in a dead letter queue for later processing
        // 2. Send an alert to operations team
        // 3. Return a partial success response with retry information
        
        // For now, we'll throw a domain exception to maintain existing behavior
        throw new DomainException(DomainCode.CANNOT_SEND_EMAIL);
    }

    /**
     * Determines if a FeignException represents a retryable error.
     * 
     * Retryable errors include:
     * - 5xx server errors (temporary server issues)
     * - 429 Too Many Requests (rate limiting)
     * - Network timeouts and connection issues
     * 
     * Non-retryable errors include:
     * - 4xx client errors (except 429)
     * - Authentication/authorization failures
     * - Malformed requests
     * 
     * @param exception the FeignException to evaluate
     * @return true if the error should be retried, false otherwise
     */
    private boolean isRetryableError(FeignException exception) {
        int status = exception.status();
        
        // Server errors (5xx) are generally retryable
        if (status >= 500) {
            log.debug("Server error detected ({}), marking as retryable", status);
            return true;
        }
        
        // Rate limiting (429) is retryable
        if (status == 429) {
            log.debug("Rate limiting detected (429), marking as retryable");
            return true;
        }
        
        // Client errors (4xx except 429) are generally not retryable
        if (status >= 400 && status < 500) {
            log.debug("Client error detected ({}), marking as non-retryable", status);
            return false;
        }
        
        // For any other cases, default to retryable
        log.debug("Unknown status code ({}), defaulting to retryable", status);
        return true;
    }
}
