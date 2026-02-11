package com.smartrent.config.otp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Zalo ZNS provider
 * 
 * Required environment variables:
 * - ZALO_ACCESS_TOKEN: Access token from Zalo Open API
 * - ZALO_OA_ID: Official Account ID
 * - ZALO_TEMPLATE_ID: Template ID for OTP messages
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "otp.providers.zalo")
public class ZaloProperties {

    /**
     * Enable/disable Zalo provider
     */
    private boolean enabled = false;

    /**
     * Zalo access token (from environment variable)
     */
    private String accessToken;

    /**
     * Zalo Official Account ID
     */
    private String oaId;

    /**
     * Template ID for OTP messages
     * Must be registered and approved by Zalo
     */
    private String templateId;

    /**
     * API endpoint (default: Zalo production endpoint)
     */
    private String apiEndpoint = "https://business.openapi.zalo.me/message/template";

    /**
     * Maximum retry attempts for failed requests
     */
    private int maxRetryAttempts = 3;

    /**
     * Retry backoff duration in seconds
     */
    private int retryBackoffSeconds = 1;

    /**
     * Request timeout in seconds
     */
    private int requestTimeoutSeconds = 10;

    /**
     * Application name to display in OTP message
     */
    private String appName = "SmartRent";

    /**
     * Default OTP expiry time in minutes
     */
    private int defaultExpiryMinutes = 5;
}

