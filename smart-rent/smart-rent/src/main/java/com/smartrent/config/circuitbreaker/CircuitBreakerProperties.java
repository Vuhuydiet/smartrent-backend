package com.smartrent.config.circuitbreaker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for circuit breaker settings.
 * These properties control the circuit breaker behavior for email service operations.
 */
@Data
@Component
@ConfigurationProperties(prefix = "application.circuit-breaker")
public class CircuitBreakerProperties {

    /**
     * Failure rate threshold in percentage.
     * Circuit breaker opens when failure rate exceeds this threshold.
     * Default: 60% (circuit opens when 60% of calls fail)
     */
    private int failureRateThreshold = 60;

    /**
     * Wait duration in open state in seconds.
     * How long the circuit breaker stays open before transitioning to half-open.
     * Default: 120 seconds (2 minutes)
     */
    private int waitDurationInOpenState = 120;

    /**
     * Sliding window size for failure rate calculation.
     * Number of calls to consider when calculating failure rate.
     * Default: 20 calls
     */
    private int slidingWindowSize = 20;

    /**
     * Minimum number of calls before circuit breaker can open.
     * Circuit breaker won't open until at least this many calls have been made.
     * Default: 10 calls
     */
    private int minimumNumberOfCalls = 10;

    /**
     * Number of permitted calls in half-open state.
     * How many test calls are allowed when circuit breaker is half-open.
     * Default: 5 calls
     */
    private int permittedCallsInHalfOpen = 5;

    /**
     * Timeout duration for individual calls in seconds.
     * Maximum time to wait for a single email sending operation.
     * Default: 15 seconds
     */
    private int timeoutDuration = 15;

    /**
     * Validates the configuration properties.
     * This method is called after properties are bound to ensure valid values.
     */
    public void validate() {
        if (failureRateThreshold < 1 || failureRateThreshold > 100) {
            throw new IllegalArgumentException("failureRateThreshold must be between 1 and 100");
        }
        if (waitDurationInOpenState < 1) {
            throw new IllegalArgumentException("waitDurationInOpenState must be at least 1 second");
        }
        if (slidingWindowSize < 1) {
            throw new IllegalArgumentException("slidingWindowSize must be at least 1");
        }
        if (minimumNumberOfCalls < 1) {
            throw new IllegalArgumentException("minimumNumberOfCalls must be at least 1");
        }
        if (permittedCallsInHalfOpen < 1) {
            throw new IllegalArgumentException("permittedCallsInHalfOpen must be at least 1");
        }
        if (timeoutDuration < 1) {
            throw new IllegalArgumentException("timeoutDuration must be at least 1 second");
        }
        if (minimumNumberOfCalls > slidingWindowSize) {
            throw new IllegalArgumentException("minimumNumberOfCalls cannot be greater than slidingWindowSize");
        }
    }
}
