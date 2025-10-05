package com.smartrent.infra.repository;

import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import com.smartrent.infra.repository.entity.Payment;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by transaction reference
     */
    Optional<Payment> findByTransactionRef(String transactionRef);

    /**
     * Find payment by VNPay transaction ID
     */
    Optional<Payment> findByVnpayTransactionId(String vnpayTransactionId);

    /**
     * Find payments by user ID
     */
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find payments by user ID and status
     */
    Page<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TransactionStatus status, Pageable pageable);

    /**
     * Find payments by listing ID
     */
    Page<Payment> findByListingIdOrderByCreatedAtDesc(Long listingId, Pageable pageable);

    /**
     * Find payments by status
     */
    Page<Payment> findByStatusOrderByCreatedAtDesc(TransactionStatus status, Pageable pageable);

    /**
     * Find payments by transaction type
     */
    Page<Payment> findByTransactionTypeOrderByCreatedAtDesc(TransactionType transactionType, Pageable pageable);

    /**
     * Find payments by user ID and transaction types (for credit transactions)
     */
    Page<Payment> findByUserIdAndTransactionTypeInOrderByCreatedAtDesc(Long userId, List<TransactionType> transactionTypes, Pageable pageable);

    /**
     * Check if transaction reference exists
     */
    boolean existsByTransactionRef(String transactionRef);

    /**
     * Find payments by date range
     */
    @Query("SELECT p FROM payments p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    Page<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);

    /**
     * Find pending payments older than specified time
     */
    @Query("SELECT p FROM payments p WHERE p.status = :status AND p.createdAt < :cutoffTime")
    List<Payment> findPendingPaymentsOlderThan(@Param("status") TransactionStatus status,
                                               @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count payments by user and status
     */
    long countByUserIdAndStatus(Long userId, TransactionStatus status);

    /**
     * Find successful payments by user ID
     */
    @Query("SELECT p FROM payments p WHERE p.userId = :userId AND p.status = 'SUCCESS' ORDER BY p.createdAt DESC")
    List<Payment> findSuccessfulPaymentsByUserId(@Param("userId") Long userId);

    /**
     * Find payments by multiple statuses
     */
    @Query("SELECT p FROM payments p WHERE p.status IN :statuses ORDER BY p.createdAt DESC")
    Page<Payment> findByStatusIn(@Param("statuses") List<TransactionStatus> statuses, Pageable pageable);
}
