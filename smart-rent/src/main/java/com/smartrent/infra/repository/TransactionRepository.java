package com.smartrent.infra.repository;

import com.smartrent.enums.ReferenceType;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import com.smartrent.infra.repository.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByUserId(String userId);

    Page<Transaction> findByUserId(String userId, Pageable pageable);

    List<Transaction> findByUserIdAndTransactionType(String userId, TransactionType transactionType);

    List<Transaction> findByUserIdAndStatus(String userId, TransactionStatus status);

    List<Transaction> findByUserIdAndTransactionTypeAndStatus(String userId, TransactionType transactionType, TransactionStatus status);

    Optional<Transaction> findByProviderTransactionId(String providerTransactionId);

    List<Transaction> findByReferenceTypeAndReferenceId(ReferenceType referenceType, String referenceId);

    @Query("SELECT t FROM transactions t WHERE t.userId = :userId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM transactions t WHERE t.userId = :userId AND t.transactionType = :type AND t.status = 'COMPLETED'")
    Long getTotalAmountByUserAndType(@Param("userId") String userId, @Param("type") TransactionType type);

    @Query("SELECT t FROM transactions t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT t FROM transactions t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    @Query("SELECT t FROM transactions t WHERE t.userId = :userId AND t.transactionType = :type ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(@Param("userId") String userId, @Param("type") TransactionType type);

    // Payment-specific methods (using transactionId as transactionRef)
    Optional<Transaction> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    @Query("SELECT t FROM transactions t WHERE t.userId = :userId AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdAndStatusOrderByCreatedAtDesc(@Param("userId") String userId, @Param("status") TransactionStatus status, Pageable pageable);

    // ─── Admin Dashboard: Revenue Over Time ───

    @Query(value = "SELECT DATE(t.created_at) AS revenue_date, SUM(t.amount) AS total_amount, COUNT(*) AS tx_count " +
            "FROM transactions t " +
            "WHERE t.status = 'COMPLETED' AND t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(t.created_at) ORDER BY revenue_date ASC", nativeQuery = true)
    List<Object[]> findDailyRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT t.transaction_type, SUM(t.amount) AS total_amount, COUNT(*) AS tx_count " +
            "FROM transactions t " +
            "WHERE t.status = 'COMPLETED' AND t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.transaction_type ORDER BY total_amount DESC", nativeQuery = true)
    List<Object[]> findRevenueGroupedByType(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

