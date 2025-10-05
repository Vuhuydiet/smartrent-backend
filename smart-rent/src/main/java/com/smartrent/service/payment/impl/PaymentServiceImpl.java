package com.smartrent.service.payment.impl;

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
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import com.smartrent.enums.WalletTransactionType;
import com.smartrent.enums.WalletReferenceType;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.UserWalletRepository;
import com.smartrent.infra.repository.WalletTransactionRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.infra.repository.entity.UserWallet;
import com.smartrent.infra.repository.entity.WalletTransaction;
import com.smartrent.mapper.PaymentMapper;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.infra.repository.PaymentRepository;
import com.smartrent.infra.repository.entity.Payment;
import com.smartrent.service.payment.PaymentService;
import com.smartrent.service.payment.provider.PaymentProviderFactory;
import com.smartrent.service.payment.provider.PaymentProvider.PaymentFeature;
import com.smartrent.utility.PaymentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {

    PaymentProviderFactory paymentProviderFactory;
    PaymentRepository paymentRepository;
    PaymentMapper paymentMapper;
    UserRepository userRepository;
    UserWalletRepository userWalletRepository;
    WalletTransactionRepository walletTransactionRepository;

    // Generic Payment Methods (Provider-agnostic)

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest) {
        log.info("Creating payment for provider: {} with amount: {}", request.getProvider(), request.getAmount());

        try {
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(request.getProvider());

            return provider.createPayment(request, httpRequest);
        } catch (Exception e) {
            log.error("Error creating payment for provider: {}", request.getProvider(), e);
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResponse processCallback(PaymentCallbackRequest request, HttpServletRequest httpRequest) {
        String txnRef = extractTransactionRef(request.getParams(), request.getProvider());
        log.info("Processing callback for provider: {} and transaction: {}", request.getProvider(), txnRef);

        try {
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(request.getProvider());

            return provider.processCallback(request.getParams(), httpRequest);
        } catch (Exception e) {
            log.error("Error processing callback for provider: {}", request.getProvider(), e);
            throw new RuntimeException("Failed to process callback", e);
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResponse processIPN(PaymentCallbackRequest request, HttpServletRequest httpRequest) {
        String txnRef = extractTransactionRef(request.getParams(), request.getProvider());
        log.info("Processing IPN for provider: {} and transaction: {}", request.getProvider(), txnRef);

        try {
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(request.getProvider());

            return provider.processIPN(request.getParams(), httpRequest);
        } catch (Exception e) {
            log.error("Error processing IPN for provider: {}", request.getProvider(), e);
            throw new RuntimeException("Failed to process IPN", e);
        }
    }

    @Override
    public PaymentCallbackResponse queryTransaction(String transactionRef) {
        log.info("Querying transaction: {}", transactionRef);

        try {
            PaymentProvider providerType = determineProviderFromTransactionRef(transactionRef);
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(providerType);

            return provider.queryTransaction(transactionRef);
        } catch (Exception e) {
            log.error("Error querying transaction: {}", transactionRef, e);
            throw new RuntimeException("Failed to query transaction", e);
        }
    }

    @Override
    public PaymentCallbackResponse refundPayment(PaymentRefundRequest request) {
        log.info("Refunding payment: {} with amount: {}", request.getTransactionRef(), request.getAmount());

        try {
            PaymentProvider providerType = determineProviderFromTransactionRef(request.getTransactionRef());
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(providerType);

            return provider.refundPayment(request.getTransactionRef(), request.getAmount(), request.getReason());
        } catch (Exception e) {
            log.error("Error refunding payment: {}", request.getTransactionRef(), e);
            throw new RuntimeException("Failed to refund payment", e);
        }
    }


    // Payment Management Methods

    @Override
    public Payment getPaymentByTransactionRef(String transactionRef) {
        return paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + transactionRef));
    }

    @Override
    public Page<PaymentHistoryResponse> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(paymentMapper::toPaymentHistoryResponse);
    }

    @Override
    public Page<PaymentHistoryResponse> getPaymentHistoryByStatus(PaymentHistoryByStatusRequest request) {
        return paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                request.getUserId(), request.getStatus(), request.getPageable())
                .map(paymentMapper::toPaymentHistoryResponse);
    }

    @Override
    @Transactional
    public boolean cancelPayment(String transactionRef, String reason) {
        log.info("Cancelling payment: {}", transactionRef);

        try {
            PaymentProvider providerType = determineProviderFromTransactionRef(transactionRef);
            com.smartrent.service.payment.provider.PaymentProvider provider =
                    paymentProviderFactory.getProvider(providerType);

            return provider.cancelPayment(transactionRef, reason);
        } catch (Exception e) {
            log.error("Error cancelling payment: {}", transactionRef, e);
            return false;
        }
    }

    @Override
    public boolean transactionRefExists(String transactionRef) {
        return paymentRepository.existsByTransactionRef(transactionRef);
    }

    @Override
    @Transactional
    public Payment updatePaymentStatus(PaymentStatusUpdateRequest request) {
        Payment payment = getPaymentByTransactionRef(request.getTransactionRef());
        TransactionStatus oldStatus = payment.getStatus();
        payment.setStatus(request.getStatus());
        if (request.getStatus().isSuccess()) {
            payment.setPaymentDate(java.time.LocalDateTime.now());
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Automatically add credit to user wallet if payment becomes successful
        if (!oldStatus.isSuccess() && request.getStatus().isSuccess()) {
            addCreditForSuccessfulPayment(savedPayment);
        }

        return savedPayment;
    }

    // Provider Management Methods

    @Override
    public List<PaymentProvider> getAvailableProviders() {
        return paymentProviderFactory.getAllProviders().stream()
                .map(com.smartrent.service.payment.provider.PaymentProvider::getProviderType)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentProvider> getProvidersByFeature(PaymentFeature feature) {
        return paymentProviderFactory.getProvidersByFeature(feature).stream()
                .map(com.smartrent.service.payment.provider.PaymentProvider::getProviderType)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isProviderAvailable(PaymentProvider provider) {
        return paymentProviderFactory.isProviderAvailable(provider);
    }

    @Override
    public Map<PaymentProvider, Map<String, Object>> getProviderSchemas() {
        return paymentProviderFactory.getProviderSchemas();
    }

    // Credit/Wallet Management Methods Implementation

    @Override
    @Transactional
    public CreditTransactionResponse addUserCredit(AddCreditRequest request) {
        log.info("Adding credit: {} {} to user: {}", request.getAmount(), request.getCurrency(), request.getUserId());

        try {
            // Get or create user wallet
            UserWallet wallet = getOrCreateUserWallet(request.getUserId().toString(), request.getCurrency());
            BigDecimal oldBalance = wallet.getCreditBalance();

            // Add credit to wallet
            wallet.addCredit(request.getAmount());
            UserWallet savedWallet = userWalletRepository.save(wallet);

            // Create wallet transaction record
            WalletTransaction transaction = WalletTransaction.builder()
                    .walletId(savedWallet.getWalletId())
                    .userId(request.getUserId().toString())
                    .transactionType(WalletTransactionType.CREDIT_ADD)
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                    .balanceBefore(oldBalance)
                    .balanceAfter(savedWallet.getCreditBalance())
                    .referenceType(WalletReferenceType.MANUAL)
                    .referenceId(request.getReferenceTransactionId())
                    .description("Credit added to wallet")
                    .reason(request.getReason())
                    .createdBy("SYSTEM")
                    .build();

            WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

            log.info("Successfully added credit to user: {}. New balance: {}", request.getUserId(), savedWallet.getCreditBalance());

            return CreditTransactionResponse.builder()
                    .transactionId(savedTransaction.getTransactionId())
                    .userId(request.getUserId())
                    .transactionType("CREDIT_ADD")
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                    .balanceAfter(savedWallet.getCreditBalance())
                    .reason(request.getReason())
                    .referenceTransactionId(request.getReferenceTransactionId())
                    .transactionDate(LocalDateTime.now())
                    .success(true)
                    .message("Credit added successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error adding credit to user: {}", request.getUserId(), e);
            return CreditTransactionResponse.builder()
                    .userId(request.getUserId())
                    .transactionType("CREDIT_ADD")
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                    .success(false)
                    .message("Failed to add credit: " + e.getMessage())
                    .transactionDate(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public CreditTransactionResponse subtractUserCredit(SubtractCreditRequest request) {
        log.info("Subtracting credit: {} {} from user: {}", request.getAmount(), request.getCurrency(), request.getUserId());

        try {
            // Get user wallet
            UserWallet wallet = userWalletRepository.findByUserIdAndIsActiveTrue(request.getUserId().toString())
                    .orElseThrow(() -> new RuntimeException("Active wallet not found for user: " + request.getUserId()));

            BigDecimal oldBalance = wallet.getCreditBalance();

            // Check sufficient balance and subtract credit
            if (!wallet.subtractCredit(request.getAmount())) {
                log.warn("Insufficient credit balance for user: {}. Current: {}, Requested: {}",
                        request.getUserId(), oldBalance, request.getAmount());
                return CreditTransactionResponse.builder()
                        .userId(request.getUserId())
                        .transactionType("CREDIT_SUBTRACT")
                        .amount(request.getAmount())
                        .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                        .balanceAfter(oldBalance)
                        .success(false)
                        .message("Insufficient credit balance")
                        .transactionDate(LocalDateTime.now())
                        .build();
            }

            UserWallet savedWallet = userWalletRepository.save(wallet);

            // Create wallet transaction record
            WalletTransaction transaction = WalletTransaction.builder()
                    .walletId(savedWallet.getWalletId())
                    .userId(request.getUserId().toString())
                    .transactionType(WalletTransactionType.CREDIT_SUBTRACT)
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                    .balanceBefore(oldBalance)
                    .balanceAfter(savedWallet.getCreditBalance())
                    .referenceType(WalletReferenceType.MANUAL)
                    .referenceId(request.getReferenceTransactionId())
                    .description("Credit deducted from wallet")
                    .reason(request.getReason())
                    .createdBy("SYSTEM")
                    .build();

            WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

            log.info("Successfully subtracted credit from user: {}. New balance: {}", request.getUserId(), savedWallet.getCreditBalance());

            return CreditTransactionResponse.builder()
                    .transactionId(savedTransaction.getTransactionId())
                    .userId(request.getUserId())
                    .transactionType("CREDIT_SUBTRACT")
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                    .balanceAfter(savedWallet.getCreditBalance())
                    .reason(request.getReason())
                    .referenceTransactionId(request.getReferenceTransactionId())
                    .transactionDate(LocalDateTime.now())
                    .success(true)
                    .message("Credit subtracted successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error subtracting credit from user: {}", request.getUserId(), e);
            return CreditTransactionResponse.builder()
                    .userId(request.getUserId())
                    .transactionType("CREDIT_SUBTRACT")
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                    .success(false)
                    .message("Failed to subtract credit: " + e.getMessage())
                    .transactionDate(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public CreditBalanceResponse getUserCreditBalance(Long userId) {
        log.info("Getting credit balance for user: {}", userId);

        try {
            UserWallet wallet = userWalletRepository.findByUserIdAndIsActiveTrue(userId.toString())
                    .orElse(UserWallet.builder()
                            .userId(userId.toString())
                            .creditBalance(BigDecimal.ZERO)
                            .totalCreditsAdded(BigDecimal.ZERO)
                            .totalCreditsSpent(BigDecimal.ZERO)
                            .currency("VND")
                            .build());

            return CreditBalanceResponse.builder()
                    .userId(userId)
                    .balance(wallet.getCreditBalance())
                    .currency(wallet.getCurrency())
                    .lastUpdated(wallet.getUpdatedAt())
                    .totalCreditsAdded(wallet.getTotalCreditsAdded())
                    .totalCreditsSpent(wallet.getTotalCreditsSpent())
                    .build();

        } catch (Exception e) {
            log.error("Error getting credit balance for user: {}", userId, e);
            throw new RuntimeException("Failed to get credit balance", e);
        }
    }

    @Override
    public boolean hasSufficientCredit(Long userId, BigDecimal amount) {
        try {
            UserWallet wallet = userWalletRepository.findByUserIdAndIsActiveTrue(userId.toString())
                    .orElse(null);

            return wallet != null && wallet.hasSufficientBalance(amount);

        } catch (Exception e) {
            log.error("Error checking credit balance for user: {}", userId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public void addCreditForSuccessfulPayment(Payment payment) {
        log.info("Adding credit for successful payment: {} with amount: {}", payment.getTransactionRef(), payment.getAmount());

        try {
            // Only add credit for successful payment transactions
            if (payment.isSuccessful() && TransactionType.PAYMENT.equals(payment.getTransactionType())) {
                // Get or create user wallet
                UserWallet wallet = getOrCreateUserWallet(payment.getUserId().toString(), payment.getCurrency());
                BigDecimal oldBalance = wallet.getCreditBalance();

                // Add credit to wallet
                wallet.addCredit(payment.getAmount());
                UserWallet savedWallet = userWalletRepository.save(wallet);

                // Create wallet transaction record
                WalletTransaction transaction = WalletTransaction.builder()
                        .walletId(savedWallet.getWalletId())
                        .userId(payment.getUserId().toString())
                        .transactionType(WalletTransactionType.PAYMENT_CREDIT)
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .balanceBefore(oldBalance)
                        .balanceAfter(savedWallet.getCreditBalance())
                        .referenceType(WalletReferenceType.PAYMENT)
                        .referenceId(payment.getTransactionRef())
                        .description("Credit added for successful payment")
                        .reason("Automatic credit addition for payment")
                        .createdBy("SYSTEM")
                        .build();

                walletTransactionRepository.save(transaction);
                log.info("Successfully added credit for payment: {}", payment.getTransactionRef());
            }
        } catch (Exception e) {
            log.error("Error adding credit for payment: {}", payment.getTransactionRef(), e);
            // Don't throw exception here to avoid breaking the payment flow
        }
    }

    @Override
    public Page<PaymentHistoryResponse> getCreditTransactionHistory(Long userId, Pageable pageable) {
        log.info("Getting credit transaction history for user: {}", userId);

        try {
            // Find all credit-related wallet transactions for the user
            Page<WalletTransaction> walletTransactions = walletTransactionRepository
                    .findByUserIdOrderByCreatedAtDesc(userId.toString(), pageable);

            // Convert wallet transactions to payment history response format
            return walletTransactions.map(this::convertWalletTransactionToPaymentHistory);

        } catch (Exception e) {
            log.error("Error getting credit transaction history for user: {}", userId, e);
            throw new RuntimeException("Failed to get credit transaction history", e);
        }
    }

    // Helper methods for wallet management

    private UserWallet getOrCreateUserWallet(String userId, String currency) {
        return userWalletRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseGet(() -> {
                    // Verify user exists
                    userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                    // Create new wallet for user
                    UserWallet newWallet = UserWallet.builder()
                            .userId(userId)
                            .creditBalance(BigDecimal.ZERO)
                            .totalCreditsAdded(BigDecimal.ZERO)
                            .totalCreditsSpent(BigDecimal.ZERO)
                            .currency(currency != null ? currency : "VND")
                            .isActive(true)
                            .build();

                    return userWalletRepository.save(newWallet);
                });
    }

    private PaymentHistoryResponse convertWalletTransactionToPaymentHistory(WalletTransaction transaction) {
        return PaymentHistoryResponse.builder()
                .id(transaction.getTransactionId())
                .transactionRef("WALLET_" + transaction.getTransactionId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionType(convertWalletTransactionType(transaction.getTransactionType()))
                .status(TransactionStatus.SUCCESS)
                .orderInfo(transaction.getDescription())
                .paymentMethod("WALLET")
                .paymentDate(transaction.getCreatedAt())
                .userId(Long.valueOf(transaction.getUserId()))
                .createdAt(transaction.getCreatedAt())
                .notes(transaction.getReason())
                .build();
    }

    private TransactionType convertWalletTransactionType(WalletTransactionType walletType) {
        switch (walletType) {
            case CREDIT_ADD:
            case PAYMENT_CREDIT:
            case REFUND_CREDIT:
                return TransactionType.CREDIT_ADD;
            case CREDIT_SUBTRACT:
                return TransactionType.CREDIT_SUBTRACT;
            case ADJUSTMENT:
                return TransactionType.CREDIT_ADD; // Default to credit add for adjustments
            default:
                return TransactionType.CREDIT_ADD;
        }
    }

    // Private helper methods

    private PaymentProvider determineProviderFromTransactionRef(String transactionRef) {
        if (transactionRef.startsWith("VNPAY_")) {
            return PaymentProvider.VNPAY;
        } else if (transactionRef.startsWith("PAYPAL_")) {
            return PaymentProvider.PAYPAL;
        } else if (transactionRef.startsWith("MOMO_")) {
            return PaymentProvider.MOMO;
        }

        // Fallback: check database for payment record
        paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + transactionRef));

        // Default to VNPay if no prefix found (for backward compatibility)
        return PaymentProvider.VNPAY;
    }

    private String extractTransactionRef(Map<String, String> params, PaymentProvider providerType) {
        switch (providerType) {
            case VNPAY:
                return params.get("vnp_TxnRef");
            case PAYPAL:
                return params.get("token");
            case MOMO:
                return params.get("orderId");
            default:
                return null;
        }
    }


}