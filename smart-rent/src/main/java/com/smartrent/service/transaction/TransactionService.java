package com.smartrent.service.transaction;

import com.smartrent.dto.response.TransactionResponse;
import com.smartrent.enums.ReferenceType;
import com.smartrent.enums.TransactionType;
import com.smartrent.infra.repository.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for transaction management
 */
public interface TransactionService {

    /**
     * Create a membership purchase transaction
     * 
     * @param userId User ID
     * @param membershipId Membership package ID
     * @param amount Payment amount
     * @param paymentProvider Payment provider (VNPAY, etc.)
     * @return Transaction ID
     */
    String createMembershipTransaction(String userId, Long membershipId, BigDecimal amount, String paymentProvider);

    /**
     * Create a post fee transaction (pay-per-post)
     *
     * @param userId User ID
     * @param amount Payment amount
     * @param vipType VIP type (VIP or PREMIUM)
     * @param durationDays Duration in days
     * @param paymentProvider Payment provider
     * @return Transaction ID
     */
    String createPostFeeTransaction(String userId, BigDecimal amount, String vipType, int durationDays, String paymentProvider);

    /**
     * Create a boost fee transaction (pay-per-boost)
     * 
     * @param userId User ID
     * @param listingId Listing ID
     * @param amount Payment amount
     * @param paymentProvider Payment provider
     * @return Transaction ID
     */
    String createBoostFeeTransaction(String userId, Long listingId, BigDecimal amount, String paymentProvider);

    /**
     * Complete a transaction after successful payment
     * 
     * @param transactionId Transaction ID
     * @param providerTransactionId Provider's transaction ID
     * @return Updated transaction response
     */
    TransactionResponse completeTransaction(String transactionId, String providerTransactionId);

    /**
     * Fail a transaction
     * 
     * @param transactionId Transaction ID
     * @param reason Failure reason
     * @return Updated transaction response
     */
    TransactionResponse failTransaction(String transactionId, String reason);

    /**
     * Get transaction by ID
     *
     * @param transactionId Transaction ID
     * @return Transaction entity
     */
    Transaction getTransaction(String transactionId);

    /**
     * Get transaction history for a user
     *
     * @param userId User ID
     * @return List of transactions
     */
    List<Transaction> getTransactionHistory(String userId);

    /**
     * Get transaction history for a user filtered by type
     *
     * @param userId User ID
     * @param transactionType Transaction type
     * @return List of transactions
     */
    List<Transaction> getTransactionHistory(String userId, TransactionType transactionType);

    /**
     * Get transaction by provider transaction ID
     *
     * @param providerTransactionId Provider's transaction ID
     * @return Transaction response
     */
    TransactionResponse getTransactionByProviderTxId(String providerTransactionId);
}

