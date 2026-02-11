package com.smartrent.config.otp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for OTP service
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {

    /**
     * OTP code length (default: 6)
     */
    private int codeLength = 6;

    /**
     * OTP TTL in seconds (default: 300 = 5 minutes)
     */
    private int ttlSeconds = 300;

    /**
     * Maximum verification attempts per OTP (default: 5)
     */
    private int maxVerificationAttempts = 5;

    /**
     * Store type: redis or memory (default: redis)
     */
    private StoreConfig store = new StoreConfig();

    /**
     * Rate limiting configuration
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @Data
    public static class StoreConfig {
        private String type = "redis";
    }

    @Data
    public static class RateLimitConfig {
        /**
         * Maximum OTP sends per phone number in the time window
         */
        private int maxSendsPerPhone = 5;

        /**
         * Maximum OTP sends per IP address in the time window
         */
        private int maxSendsPerIp = 20;

        /**
         * Time window for rate limiting in seconds (default: 3600 = 1 hour)
         */
        private int windowSeconds = 3600;
    }
}

