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

    @Column(name = "payment_method", length = 50)
    String paymentMethod;

    @Column(name = "gateway_response_code", length = 50)
    String gatewayResponseCode;

    @Column(name = "gateway_bank_code", length = 50)
    String gatewayBankCode;

    @Column(name = "gateway_bank_transaction_id", length = 100)
    String gatewayBankTransactionId;

    @Column(name = "failure_reason", length = 500)
    String failureReason;

    @Column(name = "idempotency_key", length = 120)
    String idempotencyKey;

    @Column(name = "invoice_id", length = 36)
    String invoiceId;

    @Column(name = "invoice_code", length = 50)
    String invoiceCode;

    @Column(name = "landlord_id", length = 36)
    String landlordId;

    @Column(name = "room_id")
    Long roomId;

    @Column(name = "room_code", length = 50)
    String roomCode;

    @Column(name = "room_name", length = 150)
    String roomName;

    @Column(name = "room_address", length = 500)
    String roomAddress;

    @Column(name = "customer_name_snapshot", length = 150)
    String customerNameSnapshot;

    @Column(name = "customer_phone_snapshot", length = 30)
    String customerPhoneSnapshot;

    @Column(name = "landlord_name_snapshot", length = 150)
    String landlordNameSnapshot;

    @Column(name = "landlord_phone_snapshot", length = 30)
    String landlordPhoneSnapshot;

    @Column(name = "provider_payload", columnDefinition = "TEXT")
    String providerPayload;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @Column(name = "expired_at")
    LocalDateTime expiredAt;

    @Version
    @Column(name = "version")
    Long version;

    /**
     * For membership upgrade transactions, stores the ID of the membership being upgraded from
     */
    @Column(name = "previous_membership_id")
    Long previousMembershipId;

    /**
     * For upgrade transactions, stores the discount amount applied
     */
    @Column(name = "discount_amount", precision = 15, scale = 0)
    BigDecimal discountAmount;

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
        this.completedAt = LocalDateTime.now();
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

    public boolean isRepostFee() {
        return transactionType == TransactionType.REPOST_FEE;
    }

    public boolean isWalletTopup() {
        return transactionType == TransactionType.WALLET_TOPUP;
    }

    public boolean isMembershipUpgrade() {
        return transactionType == TransactionType.MEMBERSHIP_UPGRADE;
    }

    public boolean isMembershipRenewal() {
        return transactionType == TransactionType.MEMBERSHIP_RENEWAL;
    }
}

