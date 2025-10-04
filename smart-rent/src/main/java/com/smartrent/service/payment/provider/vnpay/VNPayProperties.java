package com.smartrent.service.payment.provider.vnpay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VNPayProperties {

    /**
     * VNPay Terminal Code (TMN Code)
     */
    private String tmnCode;

    /**
     * VNPay Hash Secret Key
     */
    private String hashSecret;

    /**
     * VNPay Payment URL
     */
    private String paymentUrl;

    /**
     * VNPay Query URL
     */
    private String queryUrl;

    /**
     * VNPay API Version
     */
    private String version = "2.1.0";

    /**
     * VNPay Command for payment
     */
    private String command = "pay";

    /**
     * VNPay Order Type
     */
    private String orderType = "other";

    /**
     * VNPay Currency Code
     */
    private String currencyCode = "VND";

    /**
     * VNPay Locale
     */
    private String locale = "vn";

    /**
     * VNPay Secure Hash Type
     */
    private String secureHashType = "SHA512";

    /**
     * VNPay Timeout in minutes
     */
    private Integer timeoutMinutes = 15;

    /**
     * VNPay Return URL
     */
    private String returnUrl;

    /**
     * VNPay IPN URL
     */
    private String ipnUrl;
}
