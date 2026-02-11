package com.smartrent.config.circuitbreaker;

import com.smartrent.config.retry.email.EmailRetryProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.FeignException;

import java.time.Duration;

/**
 * Configuration for Circuit Breaker and Retry patterns using Resilience4j
 * This configuration provides fault tolerance for external service calls,
 * particularly for email sending operations.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerConfig {

    private final CircuitBreakerProperties circuitBreakerProperties;
    private final EmailRetryProperties retryProperties;

    /**
     * Validates configuration properties after bean initialization
     */
    @PostConstruct
    public void init() {
        circuitBreakerProperties.validate();
        log.info("Circuit breaker configuration loaded: failureRateThreshold={}%, " +
                "waitDurationInOpenState={}s, slidingWindowSize={}, minimumNumberOfCalls={}, " +
                "permittedCallsInHalfOpen={}, timeoutDuration={}s",
                circuitBreakerProperties.getFailureRateThreshold(),
                circuitBreakerProperties.getWaitDurationInOpenState(),
                circuitBreakerProperties.getSlidingWindowSize(),
                circuitBreakerProperties.getMinimumNumberOfCalls(),
                circuitBreakerProperties.getPermittedCallsInHalfOpen(),
                circuitBreakerProperties.getTimeoutDuration());
    }

    /**
     * Default circuit breaker configuration for all services
     * This provides a baseline configuration that can be overridden for specific services
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(circuitBreakerProperties.getTimeoutDuration()))
                        .cancelRunningFuture(true)
                        .build())
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .failureRateThreshold(circuitBreakerProperties.getFailureRateThreshold())
                        .waitDurationInOpenState(Duration.ofSeconds(circuitBreakerProperties.getWaitDurationInOpenState()))
                        .slidingWindowSize(circuitBreakerProperties.getSlidingWindowSize())
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .minimumNumberOfCalls(circuitBreakerProperties.getMinimumNumberOfCalls())
                        .permittedNumberOfCallsInHalfOpenState(circuitBreakerProperties.getPermittedCallsInHalfOpen())
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .recordExceptions(FeignException.class, RuntimeException.class)
                        .build())
                .build());
    }

    /**
     * Specific circuit breaker configuration for email service
     * This provides more aggressive retry and circuit breaker settings for email operations
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> emailServiceCustomizer() {
        return factory -> {
            // Configure circuit breaker for email service
            factory.configure(builder -> builder
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(circuitBreakerProperties.getTimeoutDuration()))
                            .cancelRunningFuture(true)
                            .build())
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .failureRateThreshold(circuitBreakerProperties.getFailureRateThreshold())
                            .waitDurationInOpenState(Duration.ofSeconds(circuitBreakerProperties.getWaitDurationInOpenState()))
                            .slidingWindowSize(circuitBreakerProperties.getSlidingWindowSize())
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .minimumNumberOfCalls(circuitBreakerProperties.getMinimumNumberOfCalls())
                            .permittedNumberOfCallsInHalfOpenState(circuitBreakerProperties.getPermittedCallsInHalfOpen())
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .recordExceptions(FeignException.class, RuntimeException.class)
                            .build())

                    .build(), "emailService");

            // Add event listeners for monitoring
            factory.addCircuitBreakerCustomizer(circuitBreaker -> {
                circuitBreaker.getEventPublisher()
                        .onStateTransition(event ->
                            log.info("Email service circuit breaker state transition: {} -> {}",
                                    event.getStateTransition().getFromState(),
                                    event.getStateTransition().getToState()))
                        .onCallNotPermitted(event ->
                            log.warn("Email service call not permitted due to circuit breaker"))
                        .onError(event ->
                            log.error("Email service call failed: {}", event.getThrowable().getMessage()))
                        .onSuccess(event ->
                            log.debug("Email service call succeeded in {}ms",
                                    event.getElapsedDuration().toMillis()));
            }, "emailService");
        };
    }

    /**
     * Circuit breaker configuration customizer for fine-tuning
     * This allows for additional customization of circuit breaker behavior
     */
    @Bean
    public CircuitBreakerConfigCustomizer circuitBreakerConfigCustomizer() {
        return CircuitBreakerConfigCustomizer.of("emailService", builder -> builder
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .recordException(throwable -> {
                    // Custom logic to determine which exceptions should be recorded as failures
                    if (throwable instanceof FeignException feignException) {
                        // Don't record client errors (4xx) as failures for circuit breaker
                        return feignException.status() >= 500;
                    }
                    return true;
                }));
    }
}
