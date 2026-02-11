package com.smartrent.config.retry.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for email retry mechanism.
 * These properties control the retry behavior for email sending operations.
 */
@Data
@Component
@ConfigurationProperties(prefix = "application.email-retry")
public class EmailRetryProperties {

    /**
     * Maximum number of retry attempts for email sending.
     * Default: 3 attempts (initial + 2 retries)
     */
    private int maxAttempts;

    /**
     * Base wait duration between retry attempts in milliseconds.
     * This is the initial wait time that gets multiplied by the exponential multiplier.
     * Default: 2000ms (2 seconds)
     */
    private long baseWaitDuration;

    /**
     * Exponential backoff multiplier.
     * Each retry attempt waits baseWaitDuration * (exponentialMultiplier ^ attemptNumber).
     * Default: 2.0 (doubles the wait time for each retry)
     */
    private double exponentialMultiplier;

    /**
     * Jitter percentage to add randomness to wait times.
     * This helps prevent thundering herd problems when multiple instances retry simultaneously.
     * Value should be between 0.0 and 1.0 (0% to 100%).
     * Default: 0.25 (±25% randomness)
     */
    private double jitterPercentage;

    /**
     * Minimum wait duration in milliseconds.
     * Ensures that even with jitter, the wait time never goes below this value.
     * Default: 1000ms (1 second)
     */
    private long minWaitDuration;

    /**
     * Validates the configuration properties.
     * This method is called after properties are bound to ensure valid values.
     */
    public void validate() {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (baseWaitDuration < 0) {
            throw new IllegalArgumentException("baseWaitDuration must be non-negative");
        }
        if (exponentialMultiplier < 1.0) {
            throw new IllegalArgumentException("exponentialMultiplier must be at least 1.0");
        }
        if (jitterPercentage < 0.0 || jitterPercentage > 1.0) {
            throw new IllegalArgumentException("jitterPercentage must be between 0.0 and 1.0");
        }
        if (minWaitDuration < 0) {
            throw new IllegalArgumentException("minWaitDuration must be non-negative");
        }
    }
}
