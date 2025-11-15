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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class AbstractPaymentProvider implements PaymentProvider {

    PaymentRepository paymentRepository;

    /**
     * Create a payment record in the database
     * If transactionId is provided in the request, reuse existing transaction
     * Otherwise, create a new transaction
     */
    @Transactional
    protected Transaction createPaymentRecord(PaymentRequest request, HttpServletRequest httpRequest) {
        validatePaymentRequest(request);

        // If transactionId is provided, reuse existing transaction
        if (request.getTransactionId() != null && !request.getTransactionId().isEmpty()) {
            Optional<Transaction> existingTransaction = paymentRepository.findByTransactionRef(request.getTransactionId());
            if (existingTransaction.isPresent()) {
                Transaction transaction = existingTransaction.get();
                // Update transaction with additional payment info if needed
                String ipAddress = getClientIpAddress(httpRequest);
                if (transaction.getIpAddress() == null || transaction.getIpAddress().equals("UNKNOWN")) {
                    transaction.setIpAddress(ipAddress);
                }
                if (transaction.getOrderInfo() == null && request.getOrderInfo() != null) {
                    transaction.setOrderInfo(request.getOrderInfo());
                }
                return paymentRepository.save(transaction);
            }
            // If transaction not found, fall through to create new one with the provided ID
        }

        String txnRef = request.getTransactionId() != null && !request.getTransactionId().isEmpty()
                ? request.getTransactionId()
                : generateTransactionRef();
        String ipAddress = getClientIpAddress(httpRequest);
        String userId = getCurrentUserId();

        // Build additional info with all payment details
        StringBuilder additionalInfo = new StringBuilder();
        additionalInfo.append("Order: ").append(request.getOrderInfo());
        additionalInfo.append(" | IP: ").append(ipAddress);

        // Handle null httpRequest gracefully (when called from service layer)
        if (httpRequest != null) {
            String userAgent = httpRequest.getHeader("User-Agent");
            if (userAgent != null) {
                additionalInfo.append(" | UserAgent: ").append(userAgent);
            }
        }

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
        log.info("=== STARTING updatePaymentWithProviderData ===");
        log.info("Updating payment with provider data - txnRef: {}, status: {}, responseCode: {}", 
                 transactionRef, status, responseCode);
        
        Optional<Transaction> transactionOpt = paymentRepository.findByTransactionRef(transactionRef);
        if (transactionOpt.isEmpty()) {
            log.error("Transaction not found for txnRef: {}", transactionRef);
            throw new PaymentNotFoundException(transactionRef);
        }

        Transaction transaction = transactionOpt.get();
        log.info("Found transaction - txnId: {}, current status: {}, new status: {}", 
                 transaction.getTransactionId(), transaction.getStatus(), status);
        
        transaction.setProviderTransactionId(providerTransactionId);
        transaction.setStatus(status);
        
        log.info("After setStatus - transaction status is now: {}", transaction.getStatus());

        // Append provider-specific data to additional info
        String currentInfo = transaction.getAdditionalInfo() != null ? transaction.getAdditionalInfo() : "";
        StringBuilder providerInfo = new StringBuilder(currentInfo);
        providerInfo.append(" | PaymentMethod: ").append(paymentMethod);
        providerInfo.append(" | BankCode: ").append(bankCode);
        providerInfo.append(" | BankTxnId: ").append(bankTransactionId);
        providerInfo.append(" | Response: ").append(responseCode).append(" - ").append(responseMessage);
        transaction.setAdditionalInfo(providerInfo.toString());

        log.info("Calling paymentRepository.save() for txnId: {}", transaction.getTransactionId());
        Transaction savedTransaction = paymentRepository.save(transaction);
        log.info("Transaction saved successfully - txnId: {}, status: {}", 
                 savedTransaction.getTransactionId(), savedTransaction.getStatus());
        
        // Verify the save by reading back from database
        Optional<Transaction> verifyOpt = paymentRepository.findByTransactionRef(transactionRef);
        if (verifyOpt.isPresent()) {
            log.info("VERIFICATION: Re-read transaction from DB - txnId: {}, status: {}", 
                     verifyOpt.get().getTransactionId(), verifyOpt.get().getStatus());
        }
        
        log.info("=== COMPLETED updatePaymentWithProviderData ===");
        return savedTransaction;
    }

    /**
     * Generate unique transaction reference
     */
    protected String generateTransactionRef() {
        return getProviderType().getCode().toUpperCase() + "_" + PaymentUtil.generateTxnRef();
    }

    /**
     * Get client IP address
     * Returns "UNKNOWN" if request is null (when called from service layer)
     */
    protected String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
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

        // Note: listingId is optional - not all payments are for listings (e.g., membership purchases)
    }

    /**
     * Default refund implementation (not supported)
     */
    @Override
    public com.smartrent.dto.response.PaymentCallbackResponse refundPayment(String transactionRef, String amount, String reason) {
        throw PaymentProviderException.operationNotSupported("Refund", getProviderType().getDisplayName());
    }
}
