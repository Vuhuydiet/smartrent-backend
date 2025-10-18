package com.smartrent.service.payment.provider;

import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import com.smartrent.infra.exception.PaymentNotFoundException;
import com.smartrent.infra.exception.PaymentProviderException;
import com.smartrent.infra.exception.PaymentValidationException;
import com.smartrent.infra.repository.PaymentRepository;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.utility.PaymentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class AbstractPaymentProvider implements PaymentProvider {

    PaymentRepository paymentRepository;

    /**
     * Create a payment record in the database
     */
    @Transactional
    protected Transaction createPaymentRecord(PaymentRequest request, HttpServletRequest httpRequest) {
        validatePaymentRequest(request);

        String txnRef = generateTransactionRef();
        String ipAddress = getClientIpAddress(httpRequest);
        String userId = getCurrentUserId();

        // Build additional info with all payment details
        StringBuilder additionalInfo = new StringBuilder();
        additionalInfo.append("Order: ").append(request.getOrderInfo());
        additionalInfo.append(" | IP: ").append(ipAddress);
        additionalInfo.append(" | UserAgent: ").append(httpRequest.getHeader("User-Agent"));
        if (request.getReturnUrl() != null) {
            additionalInfo.append(" | ReturnURL: ").append(request.getReturnUrl());
        }
        if (request.getCancelUrl() != null) {
            additionalInfo.append(" | CancelURL: ").append(request.getCancelUrl());
        }
        if (request.getNotes() != null) {
            additionalInfo.append(" | Notes: ").append(request.getNotes());
        }

        Transaction transaction = Transaction.builder()
                .transactionId(txnRef)
                .userId(userId)
                .transactionType(TransactionType.MEMBERSHIP_PURCHASE) // Default, can be overridden
                .amount(request.getAmount())
                .referenceType(com.smartrent.enums.ReferenceType.LISTING)
                .referenceId(request.getListingId() != null ? request.getListingId().toString() : null)
                .status(TransactionStatus.PENDING)
                .paymentProvider(request.getProvider())
                .additionalInfo(additionalInfo.toString())
                .orderInfo(request.getOrderInfo())
                .ipAddress(ipAddress)
                .build();

        return paymentRepository.save(transaction);
    }

    /**
     * Find payment by transaction reference
     */
    protected Optional<Transaction> findPaymentByTransactionRef(String transactionRef) {
        return paymentRepository.findByTransactionRef(transactionRef);
    }

    /**
     * Update payment status
     */
    @Transactional
    protected Transaction updatePaymentStatus(String transactionRef, TransactionStatus status,
                                          String responseCode, String responseMessage) {
        Optional<Transaction> transactionOpt = paymentRepository.findByTransactionRef(transactionRef);
        if (transactionOpt.isEmpty()) {
            throw new PaymentNotFoundException(transactionRef);
        }

        Transaction transaction = transactionOpt.get();
        transaction.setStatus(status);

        // Append response info to additional info
        String currentInfo = transaction.getAdditionalInfo() != null ? transaction.getAdditionalInfo() : "";
        transaction.setAdditionalInfo(currentInfo + " | Response: " + responseCode + " - " + responseMessage);

        return paymentRepository.save(transaction);
    }

    /**
     * Update payment with provider-specific data
     */
    @Transactional
    protected Transaction updatePaymentWithProviderData(String transactionRef,
                                                     String providerTransactionId,
                                                     String paymentMethod,
                                                     String bankCode,
                                                     String bankTransactionId,
                                                     TransactionStatus status,
                                                     String responseCode,
                                                     String responseMessage) {
        Optional<Transaction> transactionOpt = paymentRepository.findByTransactionRef(transactionRef);
        if (transactionOpt.isEmpty()) {
            throw new PaymentNotFoundException(transactionRef);
        }

        Transaction transaction = transactionOpt.get();
        transaction.setProviderTransactionId(providerTransactionId);
        transaction.setStatus(status);

        // Append provider-specific data to additional info
        String currentInfo = transaction.getAdditionalInfo() != null ? transaction.getAdditionalInfo() : "";
        StringBuilder providerInfo = new StringBuilder(currentInfo);
        providerInfo.append(" | PaymentMethod: ").append(paymentMethod);
        providerInfo.append(" | BankCode: ").append(bankCode);
        providerInfo.append(" | BankTxnId: ").append(bankTransactionId);
        providerInfo.append(" | Response: ").append(responseCode).append(" - ").append(responseMessage);
        transaction.setAdditionalInfo(providerInfo.toString());

        return paymentRepository.save(transaction);
    }

    /**
     * Generate unique transaction reference
     */
    protected String generateTransactionRef() {
        return getProviderType().getCode().toUpperCase() + "_" + PaymentUtil.generateTxnRef();
    }

    /**
     * Get client IP address
     */
    protected String getClientIpAddress(HttpServletRequest request) {
        return PaymentUtil.getClientIpAddress(
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getRemoteAddr()
        );
    }

    /**
     * Get current user ID from security context
     */
    protected String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw PaymentValidationException.userNotAuthenticated();
        }

        // Return the user ID as String (matches Transaction entity)
        return authentication.getName();
    }


    /**
     * Check if payment exists
     */
    protected boolean paymentExists(String transactionRef) {
        return paymentRepository.existsByTransactionRef(transactionRef);
    }

    /**
     * Default implementation for unsupported features
     */
    @Override
    public boolean supportsFeature(PaymentFeature feature) {
        return false; // Override in specific providers
    }

    /**
     * Default configuration validation
     */
    @Override
    public boolean validateConfiguration() {
        return true; // Override in specific providers
    }

    /**
     * Validate payment request
     */
    protected void validatePaymentRequest(PaymentRequest request) {
        if (request == null) {
            throw new PaymentValidationException("Payment request cannot be null");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw PaymentValidationException.invalidAmount();
        }

        if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
            throw PaymentValidationException.invalidCurrency(request.getCurrency());
        }

        if (request.getListingId() == null) {
            throw new PaymentValidationException("Listing ID cannot be null");
        }
    }

    /**
     * Default refund implementation (not supported)
     */
    @Override
    public com.smartrent.dto.response.PaymentCallbackResponse refundPayment(String transactionRef, String amount, String reason) {
        throw PaymentProviderException.operationNotSupported("Refund", getProviderType().getDisplayName());
    }
}
