package com.smartrent.infra.repository;

import com.smartrent.enums.TransactionStatus;
import com.smartrent.infra.repository.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PaymentRepository - Alias for TransactionRepository for payment-specific operations
 * This provides backward compatibility with payment service code that expects PaymentRepository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Transaction, String> {

    /**
     * Find transaction by transaction ID (used as transaction reference in payment system)
     */
    default Optional<Transaction> findByTransactionRef(String transactionRef) {
        return findById(transactionRef);
    }

    /**
     * Check if transaction exists by transaction reference
     */
    default boolean existsByTransactionRef(String transactionRef) {
        return existsById(transactionRef);
    }

    /**
     * Find transactions by user ID ordered by creation date
     */
    @Query("SELECT t FROM transactions t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId, Pageable pageable);

    /**
     * Find transactions by user ID and status ordered by creation date
     */
    @Query("SELECT t FROM transactions t WHERE t.userId = :userId AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdAndStatusOrderByCreatedAtDesc(
            @Param("userId") String userId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    /**
     * Find transaction by provider transaction ID
     */
    Optional<Transaction> findByProviderTransactionId(String providerTransactionId);
}

