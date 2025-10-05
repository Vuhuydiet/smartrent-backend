package com.smartrent.service.payment;

import com.smartrent.dto.request.AddCreditRequest;
import com.smartrent.dto.request.PaymentCallbackRequest;
import com.smartrent.dto.request.PaymentHistoryByStatusRequest;
import com.smartrent.dto.request.PaymentRefundRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.PaymentStatusUpdateRequest;
import com.smartrent.dto.request.SubtractCreditRequest;
import com.smartrent.dto.response.CreditBalanceResponse;
import com.smartrent.dto.response.CreditTransactionResponse;
import com.smartrent.dto.response.PaymentCallbackResponse;
import com.smartrent.dto.response.PaymentHistoryResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.infra.repository.entity.Payment;
import com.smartrent.service.payment.provider.PaymentProvider.PaymentFeature;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    // Generic Payment Methods (Provider-agnostic)

    /**
     * Create payment with specified provider
     */
    PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest);

    /**
     * Process payment callback from any provider
     */
    PaymentCallbackResponse processCallback(PaymentCallbackRequest request, HttpServletRequest httpRequest);

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
     * Get payment by transaction reference
     */
    Payment getPaymentByTransactionRef(String transactionRef);

    /**
     * Get payment history for user
     */
    Page<PaymentHistoryResponse> getPaymentHistory(Long userId, Pageable pageable);

    /**
     * Get payment history by status
     */
    Page<PaymentHistoryResponse> getPaymentHistoryByStatus(PaymentHistoryByStatusRequest request);

    /**
     * Cancel payment
     */
    boolean cancelPayment(String transactionRef, String reason);

    /**
     * Check if transaction reference exists
     */
    boolean transactionRefExists(String transactionRef);

    /**
     * Update payment status
     */
    Payment updatePaymentStatus(PaymentStatusUpdateRequest request);

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

    // Credit/Wallet Management Methods

    /**
     * Add credit to user wallet
     */
    CreditTransactionResponse addUserCredit(AddCreditRequest request);

    /**
     * Subtract credit from user wallet
     */
    CreditTransactionResponse subtractUserCredit(SubtractCreditRequest request);

    /**
     * Get user credit balance
     */
    CreditBalanceResponse getUserCreditBalance(Long userId);

    /**
     * Check if user has sufficient credit balance
     */
    boolean hasSufficientCredit(Long userId, BigDecimal amount);

    /**
     * Automatically add credit to user wallet when payment is successful
     * This method is called internally during payment processing
     */
    void addCreditForSuccessfulPayment(Payment payment);

    /**
     * Get credit transaction history for user
     */
    Page<PaymentHistoryResponse> getCreditTransactionHistory(Long userId, Pageable pageable);
}
