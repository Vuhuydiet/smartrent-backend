package com.smartrent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * VNPay payment gateway configuration
 */
@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VNPayConfig {

    /**
     * VNPay Terminal/Merchant Code
     */
    private String tmnCode;

    /**
     * VNPay Hash Secret for signature generation
     */
    private String hashSecret;

    /**
     * VNPay Payment Gateway URL
     */
    private String url;

    /**
     * Return URL after payment (success/failure)
     */
    private String returnUrl;

    /**
     * VNPay API Version
     */
    private String version = "2.1.0";

    /**
     * Command for payment creation
     */
    private String command = "pay";

    /**
     * Currency code (VND)
     */
    private String currencyCode = "VND";

    /**
     * Locale (vn or en)
     */
    private String locale = "vn";

    /**
     * Order type
     */
    private String orderType = "other";

    /**
     * Timeout in minutes
     */
    private Integer timeout = 15;
}

