package com.smartrent.service.payment.provider;

import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Abstract payment provider interface using Strategy pattern
 * This allows different payment gateways to be plugged in without changing the core payment service
 */
public interface PaymentProvider {

    /**
     * Get the provider type
     */
    com.smartrent.enums.PaymentProvider getProviderType();

    /**
     * Create a payment with the provider
     */
    PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest);

    /**
     * Process callback from payment provider
     */
    PaymentCallbackResponse processCallback(Map<String, String> params, HttpServletRequest httpRequest);

    /**
     * Process IPN (Instant Payment Notification) from provider
     */
    PaymentCallbackResponse processIPN(Map<String, String> params, HttpServletRequest httpRequest);

    /**
     * Query transaction status from provider
     */
    PaymentCallbackResponse queryTransaction(String transactionRef);

    /**
     * Validate provider signature/webhook
     */
    boolean validateSignature(Map<String, String> params, String signature);

    /**
     * Cancel a payment with the provider
     */
    boolean cancelPayment(String transactionRef, String reason);

    /**
     * Refund a payment with the provider
     */
    PaymentCallbackResponse refundPayment(String transactionRef, String amount, String reason);

    /**
     * Check if the provider supports a specific feature
     */
    boolean supportsFeature(PaymentFeature feature);

    /**
     * Get provider-specific configuration requirements
     */
    Map<String, Object> getConfigurationSchema();

    /**
     * Validate provider configuration
     */
    boolean validateConfiguration();

    enum PaymentFeature {
        REFUND,
        PARTIAL_REFUND,
        RECURRING_PAYMENT,
        QR_CODE,
        MOBILE_PAYMENT,
        BANK_TRANSFER,
        CREDIT_CARD,
        DEBIT_CARD,
        E_WALLET,
        INSTALLMENT
    }
}
