package com.smartrent.config.retry.email;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

/**
 * Custom failure predicate for email service circuit breaker.
 * This predicate determines which exceptions should be counted as failures
 * for the circuit breaker's failure rate calculation.
 * 
 * The predicate helps distinguish between:
 * - Transient failures that should trigger circuit breaker (5xx errors, timeouts)
 * - Client errors that shouldn't affect circuit breaker state (4xx errors)
 */
@Slf4j
public class EmailServiceFailurePredicate implements Predicate<Throwable> {

    /**
     * Tests whether the given throwable should be recorded as a failure
     * for circuit breaker purposes.
     * 
     * @param throwable the exception to evaluate
     * @return true if this should be counted as a failure, false otherwise
     */
    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof FeignException feignException) {
            int status = feignException.status();
            
            // Server errors (5xx) should be recorded as failures
            if (status >= 500) {
                log.debug("Recording server error {} as circuit breaker failure", status);
                return true;
            }
            
            // Rate limiting (429) should be recorded as failure to trigger circuit breaker
            if (status == 429) {
                log.debug("Recording rate limiting (429) as circuit breaker failure");
                return true;
            }
            
            // Client errors (4xx except 429) should not be recorded as failures
            // These are typically configuration or data issues, not service health issues
            if (status >= 400 && status < 500) {
                log.debug("Not recording client error {} as circuit breaker failure", status);
                return false;
            }
            
            // For unknown status codes, default to recording as failure
            log.debug("Recording unknown status {} as circuit breaker failure", status);
            return true;
        }
        
        // Non-Feign exceptions (timeouts, connection issues, etc.) should be recorded as failures
        log.debug("Recording non-Feign exception {} as circuit breaker failure", 
                throwable.getClass().getSimpleName());
        return true;
    }
}
