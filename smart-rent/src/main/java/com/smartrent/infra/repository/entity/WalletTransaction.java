package com.smartrent.infra.repository.entity;

import com.smartrent.enums.WalletTransactionType;
import com.smartrent.enums.WalletReferenceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "wallet_transactions")
@Table(name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_transactions_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_wallet_transactions_user_id", columnList = "user_id"),
                @Index(name = "idx_wallet_transactions_type", columnList = "transaction_type"),
                @Index(name = "idx_wallet_transactions_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_wallet_transactions_created_at", columnList = "created_at"),
                @Index(name = "idx_wallet_transactions_amount", columnList = "amount")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    Long transactionId;

    @Column(name = "wallet_id", nullable = false)
    Long walletId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", insertable = false, updatable = false)
    UserWallet wallet;

    @Column(name = "user_id", nullable = false)
    String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    WalletTransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    String currency = "VND";

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    @Builder.Default
    WalletReferenceType referenceType = WalletReferenceType.MANUAL;

    @Column(name = "reference_id")
    String referenceId;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "reason", length = 500)
    String reason;

    @Column(name = "metadata", columnDefinition = "JSON")
    String metadata;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "created_by")
    String createdBy;

    // Helper methods
    public boolean isCredit() {
        return transactionType != null &&
               (transactionType == WalletTransactionType.CREDIT_ADD ||
                transactionType == WalletTransactionType.PAYMENT_CREDIT ||
                transactionType == WalletTransactionType.REFUND_CREDIT);
    }

    public boolean isDebit() {
        return transactionType != null &&
               transactionType == WalletTransactionType.CREDIT_SUBTRACT;
    }

    public boolean isPaymentRelated() {
        return referenceType == WalletReferenceType.PAYMENT;
    }
}