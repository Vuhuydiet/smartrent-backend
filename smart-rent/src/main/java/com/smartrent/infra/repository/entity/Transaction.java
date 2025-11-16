package com.smartrent.infra.repository.entity;

import com.smartrent.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "transactions")
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_transaction_type", columnList = "transaction_type"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_created_at", columnList = "created_at"),
                @Index(name = "idx_provider_tx_id", columnList = "provider_transaction_id")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @Column(name = "transaction_id", length = 36)
    String transactionId;

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    ReferenceType referenceType;

    @Column(name = "reference_id", length = 100)
    String referenceId;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    String additionalInfo;

    @Column(name = "order_info", length = 500)
    String orderInfo;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider")
    PaymentProvider paymentProvider;

    @Column(name = "provider_transaction_id")
    String providerTransactionId;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<PushSchedule> pushSchedules;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Helper methods
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == TransactionStatus.CANCELLED;
    }

    public boolean isRefunded() {
        return status == TransactionStatus.REFUNDED;
    }

    public void complete() {
        this.status = TransactionStatus.COMPLETED;
    }

    public void fail() {
        this.status = TransactionStatus.FAILED;
    }

    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
    }

    public void refund() {
        this.status = TransactionStatus.REFUNDED;
    }

    public boolean isMembershipPurchase() {
        return transactionType == TransactionType.MEMBERSHIP_PURCHASE;
    }

    public boolean isPostFee() {
        return transactionType == TransactionType.POST_FEE;
    }

    public boolean isPushFee() {
        return transactionType == TransactionType.PUSH_FEE;
    }

    public boolean isWalletTopup() {
        return transactionType == TransactionType.WALLET_TOPUP;
    }
}

