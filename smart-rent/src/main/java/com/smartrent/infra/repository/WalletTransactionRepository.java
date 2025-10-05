package com.smartrent.infra.repository;

import com.smartrent.enums.WalletTransactionType;
import com.smartrent.enums.WalletReferenceType;
import com.smartrent.infra.repository.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Find transactions by wallet ID
     */
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Find transactions by user ID
     */
    Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find transactions by user ID and transaction type
     */
    Page<WalletTransaction> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(
            String userId, WalletTransactionType transactionType, Pageable pageable);

    /**
     * Find transactions by user ID and transaction types
     */
    Page<WalletTransaction> findByUserIdAndTransactionTypeInOrderByCreatedAtDesc(
            String userId, List<WalletTransactionType> transactionTypes, Pageable pageable);

    /**
     * Find transactions by reference
     */
    List<WalletTransaction> findByReferenceTypeAndReferenceId(WalletReferenceType referenceType, String referenceId);

    /**
     * Find transactions by date range
     */
    @Query("SELECT wt FROM wallet_transactions wt WHERE wt.createdAt BETWEEN :startDate AND :endDate ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find transactions by wallet and date range
     */
    @Query("SELECT wt FROM wallet_transactions wt WHERE wt.walletId = :walletId AND wt.createdAt BETWEEN :startDate AND :endDate ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByWalletIdAndDateRange(
            @Param("walletId") Long walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Calculate total amount by transaction type for user
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM wallet_transactions wt WHERE wt.userId = :userId AND wt.transactionType = :transactionType")
    BigDecimal getTotalAmountByUserAndType(@Param("userId") String userId, @Param("transactionType") WalletTransactionType transactionType);

    /**
     * Calculate total credit added for user
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM wallet_transactions wt WHERE wt.userId = :userId AND wt.transactionType IN ('CREDIT_ADD', 'PAYMENT_CREDIT', 'REFUND_CREDIT')")
    BigDecimal getTotalCreditsAddedByUser(@Param("userId") String userId);

    /**
     * Calculate total credit spent for user
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM wallet_transactions wt WHERE wt.userId = :userId AND wt.transactionType = 'CREDIT_SUBTRACT'")
    BigDecimal getTotalCreditsSpentByUser(@Param("userId") String userId);

    /**
     * Count transactions by user and type
     */
    long countByUserIdAndTransactionType(String userId, WalletTransactionType transactionType);

    /**
     * Find latest transaction for wallet
     */
    @Query("SELECT wt FROM wallet_transactions wt WHERE wt.walletId = :walletId ORDER BY wt.createdAt DESC LIMIT 1")
    WalletTransaction findLatestTransactionByWalletId(@Param("walletId") Long walletId);

    /**
     * Find transactions with amount greater than specified value
     */
    @Query("SELECT wt FROM wallet_transactions wt WHERE wt.amount > :amount ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findTransactionsWithAmountGreaterThan(@Param("amount") BigDecimal amount, Pageable pageable);
}