package com.smartrent.service.payment.provider;

import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import com.smartrent.infra.exception.PaymentNotFoundException;
import com.smartrent.infra.exception.PaymentProviderException;
import com.smartrent.infra.exception.PaymentValidationException;
import com.smartrent.infra.repository.PaymentRepository;
import com.smartrent.infra.repository.entity.Payment;
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
    protected Payment createPaymentRecord(PaymentRequest request, HttpServletRequest httpRequest) {
        validatePaymentRequest(request);

        String txnRef = generateTransactionRef();
        String ipAddress = getClientIpAddress(httpRequest);
        Long userId = getCurrentUserId();

        Payment payment = Payment.builder()
                .userId(userId)
                .listingId(request.getListingId())
                .transactionRef(txnRef)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .transactionType(TransactionType.PAYMENT)
                .status(TransactionStatus.PENDING)
                .orderInfo(request.getOrderInfo())
                .ipAddress(ipAddress)
                .userAgent(httpRequest.getHeader("User-Agent"))
                .returnUrl(request.getReturnUrl())
                .cancelUrl(request.getCancelUrl())
                .notes(request.getNotes())
                .metadata(request.getMetadata() != null ? request.getMetadata().toString() : null)
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Find payment by transaction reference
     */
    protected Optional<Payment> findPaymentByTransactionRef(String transactionRef) {
        return paymentRepository.findByTransactionRef(transactionRef);
    }

    /**
     * Update payment status
     */
    @Transactional
    protected Payment updatePaymentStatus(String transactionRef, TransactionStatus status,
                                          String responseCode, String responseMessage) {
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionRef(transactionRef);
        if (paymentOpt.isEmpty()) {
            throw new PaymentNotFoundException(transactionRef);
        }

        Payment payment = paymentOpt.get();
        payment.setStatus(status);

        if (status.isSuccess()) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        return paymentRepository.save(payment);
    }

    /**
     * Update payment with provider-specific data
     */
    @Transactional
    protected Payment updatePaymentWithProviderData(String transactionRef,
                                                     String providerTransactionId,
                                                     String paymentMethod,
                                                     String bankCode,
                                                     String bankTransactionId,
                                                     TransactionStatus status,
                                                     String responseCode,
                                                     String responseMessage) {
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionRef(transactionRef);
        if (paymentOpt.isEmpty()) {
            throw new PaymentNotFoundException(transactionRef);
        }

        Payment payment = paymentOpt.get();
        payment.setProviderTransactionId(providerTransactionId);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(status);

        if (status.isSuccess()) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        return paymentRepository.save(payment);
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
    protected Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw PaymentValidationException.userNotAuthenticated();
        }

        try {
            // Assuming the principal contains the user ID
            String userId = authentication.getName();
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new PaymentValidationException("Invalid user ID format in authentication");
        }
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
            throw PaymentValidationException.invalidAmount(request.getAmount() != null ? request.getAmount().toString() : "null");
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
