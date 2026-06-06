package com.smartrent.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SePay Payment Gateway (Cổng thanh toán) configuration.
 *
 * <p>SePay's Payment Gateway is a hosted-checkout product (https://developer.sepay.vn/vi/cong-thanh-toan).
 * Unlike the older bank-transfer/VietQR reconciliation product, the customer is sent to a SePay-hosted
 * checkout page via an HMAC-SHA256-signed form POST, and the result is confirmed by:
 *
 * <ul>
 *   <li>a browser redirect back to {@code successUrl}/{@code errorUrl}/{@code cancelUrl}, and</li>
 *   <li>a server-to-server IPN ({@code POST} to {@link #ipnUrl}) authenticated by an
 *       {@code X-Secret-Key: <secretKey>} header.</li>
 * </ul>
 *
 * <p>The Basic-Auth REST API ({@link #getApiBaseUrl()}) is used for order/transaction queries and
 * cancel/void operations: {@code Authorization: Basic base64(merchantId:secretKey)}.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sepay")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SePayConfig {

    /** Environment: {@code sandbox} or {@code production}. Selects the checkout/API base URLs. */
    String env = "sandbox";

    /** SePay Payment Gateway Merchant ID (from my.sepay.vn -> Payment Gateway). */
    String merchantId;

    /**
     * SePay Payment Gateway Secret Key. Used to sign the checkout form (HMAC-SHA256), as the
     * Basic-Auth password for API calls, and verified against the IPN's {@code X-Secret-Key} header.
     */
    String secretKey;

    /**
     * Pre-selected payment method on the hosted checkout:
     * {@code BANK_TRANSFER}, {@code NAPAS_BANK_TRANSFER} or {@code CARD}.
     */
    String paymentMethod = "BANK_TRANSFER";

    /** Currency code sent to the gateway. */
    String currency = "VND";

    /** Frontend URL SePay redirects the browser to after a successful payment. */
    String successUrl;

    /** Frontend URL SePay redirects the browser to after a failed payment. */
    String errorUrl;

    /** Frontend URL SePay redirects the browser to when the user cancels. */
    String cancelUrl;

    /** IPN / webhook URL SePay calls when the payment result is known (informational; set in dashboard). */
    String ipnUrl;

    /** How long the checkout / payment window is considered valid, in minutes. */
    Integer expiryMinutes = 15;

    private boolean isSandbox() {
        return env == null || !"production".equalsIgnoreCase(env.trim());
    }

    /** Hosted-checkout init URL the signed form is POSTed to. */
    public String getCheckoutInitUrl() {
        return isSandbox()
                ? "https://pay-sandbox.sepay.vn/v1/checkout/init"
                : "https://pay.sepay.vn/v1/checkout/init";
    }

    /** Base URL of the Basic-Auth REST API (orders/transactions), without a trailing slash. */
    public String getApiBaseUrl() {
        return isSandbox()
                ? "https://pgapi-sandbox.sepay.vn/v1"
                : "https://pgapi.sepay.vn/v1";
    }
}
