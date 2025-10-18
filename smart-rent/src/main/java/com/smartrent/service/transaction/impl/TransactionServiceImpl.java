package com.smartrent.service.transaction.impl;

import com.smartrent.dto.response.TransactionResponse;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.ReferenceType;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.service.transaction.TransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of TransactionService
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    TransactionRepository transactionRepository;

    @Override
    @Transactional
    public String createMembershipTransaction(String userId, Long membershipId, BigDecimal amount, String paymentProvider) {
        log.info("Creating membership transaction for user: {}, membership: {}", userId, membershipId);

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .transactionType(TransactionType.MEMBERSHIP_PURCHASE)
                .amount(amount)
                .referenceType(ReferenceType.MEMBERSHIP)
                .referenceId(membershipId.toString())
                .status(TransactionStatus.PENDING)
                .paymentProvider(PaymentProvider.valueOf(paymentProvider != null ? paymentProvider : "VNPAY"))
                .additionalInfo("Membership package purchase")
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Created membership transaction: {}", transaction.getTransactionId());
        
        return transaction.getTransactionId();
    }

    @Override
    @Transactional
    public String createPostFeeTransaction(String userId, BigDecimal amount, String vipType, int durationDays, String paymentProvider) {
        log.info("Creating post fee transaction for user: {}, vipType: {}, duration: {} days", userId, vipType, durationDays);

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .transactionType(TransactionType.POST_FEE)
                .amount(amount)
                .referenceType(ReferenceType.LISTING)
                .referenceId(vipType) // Store vipType temporarily
                .status(TransactionStatus.PENDING)
                .paymentProvider(PaymentProvider.valueOf(paymentProvider != null ? paymentProvider : "VNPAY"))
                .additionalInfo("Pay-per-post " + vipType + " listing fee for " + durationDays + " days")
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Created post fee transaction: {}", transaction.getTransactionId());

        return transaction.getTransactionId();
    }

    @Override
    @Transactional
    public String createPushFeeTransaction(String userId, Long listingId, BigDecimal amount, String paymentProvider) {
        log.info("Creating push fee transaction for user: {}, listing: {}", userId, listingId);

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .transactionType(TransactionType.PUSH_FEE)
                .amount(amount)
                .referenceType(ReferenceType.PUSH)
                .referenceId(listingId.toString())
                .status(TransactionStatus.PENDING)
                .paymentProvider(PaymentProvider.valueOf(paymentProvider != null ? paymentProvider : "VNPAY"))
                .additionalInfo("Pay-per-push fee")
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Created push fee transaction: {}", transaction.getTransactionId());

        return transaction.getTransactionId();
    }

    @Override
    @Transactional
    public TransactionResponse completeTransaction(String transactionId, String providerTransactionId) {
        log.info("Completing transaction: {}, provider tx: {}", transactionId, providerTransactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (transaction.isCompleted()) {
            log.warn("Transaction already completed: {}", transactionId);
            return mapToResponse(transaction);
        }

        transaction.complete();
        transaction.setProviderTransactionId(providerTransactionId);
        transaction = transactionRepository.save(transaction);

        log.info("Transaction completed successfully: {}", transactionId);
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse failTransaction(String transactionId, String reason) {
        log.info("Failing transaction: {}, reason: {}", transactionId, reason);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        transaction.fail();
        if (reason != null) {
            String currentInfo = transaction.getAdditionalInfo();
            transaction.setAdditionalInfo(currentInfo + " | Failure reason: " + reason);
        }
        transaction = transactionRepository.save(transaction);

        log.info("Transaction failed: {}", transactionId);
        return mapToResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(String transactionId) {
        log.info("Getting transaction: {}", transactionId);

        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String userId) {
        log.info("Getting transaction history for user: {}", userId);

        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String userId, TransactionType transactionType) {
        log.info("Getting transaction history for user: {}, type: {}", userId, transactionType);

        return transactionRepository.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(userId, transactionType);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByProviderTxId(String providerTransactionId) {
        log.info("Getting transaction by provider tx ID: {}", providerTransactionId);

        Transaction transaction = transactionRepository.findByProviderTransactionId(providerTransactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for provider tx: " + providerTransactionId));

        return mapToResponse(transaction);
    }

    // Helper methods

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .transactionType(transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null)
                .amount(transaction.getAmount())
                .referenceType(transaction.getReferenceType() != null ? transaction.getReferenceType().name() : null)
                .referenceId(transaction.getReferenceId())
                .status(transaction.getStatus() != null ? transaction.getStatus().name() : null)
                .paymentProvider(transaction.getPaymentProvider() != null ? transaction.getPaymentProvider().name() : null)
                .providerTransactionId(transaction.getProviderTransactionId())
                .additionalInfo(transaction.getAdditionalInfo())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}

