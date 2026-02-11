package com.smartrent.service.payment;

import com.smartrent.dto.request.PaymentCallbackRequest;
import com.smartrent.dto.request.PaymentHistoryByStatusRequest;
import com.smartrent.dto.request.PaymentRefundRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.PaymentStatusUpdateRequest;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.dto.response.TransactionResponse;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.service.payment.provider.PaymentProvider.PaymentFeature;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    // Generic Payment Methods (Provider-agnostic)

    /**
     * Create payment with specified provider
     */
    PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest);

    /**
     * Process IPN from any provider
     */
    PaymentCallbackResponse processIPN(PaymentCallbackRequest request, HttpServletRequest httpRequest);

    /**
     * Query transaction status from any provider
     */
    PaymentCallbackResponse queryTransaction(String transactionRef);

    /**
     * Refund payment
     */
    PaymentCallbackResponse refundPayment(PaymentRefundRequest request);

    // Payment Management Methods

    /**
     * Get transaction by transaction ID
     */
    Transaction getPaymentByTransactionRef(String transactionId);

    /**
     * Get payment history for user
     */
    Page<PaymentHistoryResponse> getPaymentHistory(String userId, Pageable pageable);

    /**
     * Get payment history by status
     */
    Page<PaymentHistoryResponse> getPaymentHistoryByStatus(PaymentHistoryByStatusRequest request);

    /**
     * Cancel payment
     */
    boolean cancelPayment(String transactionId, String reason);

    /**
     * Check if transaction reference exists
     */
    boolean transactionRefExists(String transactionId);

    /**
     * Update payment status
     */
    TransactionResponse updatePaymentStatus(PaymentStatusUpdateRequest request);

    // Provider Management Methods

    /**
     * Get available payment providers
     */
    List<PaymentProvider> getAvailableProviders();

    /**
     * Get providers that support a specific feature
     */
    List<PaymentProvider> getProvidersByFeature(PaymentFeature feature);

    /**
     * Check if provider is available
     */
    boolean isProviderAvailable(PaymentProvider provider);

    /**
     * Get provider configuration schemas
     */
    Map<PaymentProvider, Map<String, Object>> getProviderSchemas();
}
