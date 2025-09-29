package com.smartrent.infra.repository.entity;

import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "payments")
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payment_user_id", columnList = "user_id"),
                @Index(name = "idx_payment_listing_id", columnList = "listing_id"),
                @Index(name = "idx_payment_transaction_ref", columnList = "transaction_ref"),
                @Index(name = "idx_payment_provider", columnList = "provider"),
                @Index(name = "idx_payment_provider_transaction_id", columnList = "provider_transaction_id"),
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;

    @Column(name = "listing_id")
    Long listingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 100)
    String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    PaymentProvider provider;

    @Column(name = "provider_transaction_id", length = 100)
    String providerTransactionId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    TransactionStatus status;

    @Column(name = "order_info", length = 500)
    String orderInfo;

    @Column(name = "payment_method", length = 50)
    String paymentMethod;

    @Column(name = "payment_date")
    LocalDateTime paymentDate;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "user_agent", length = 500)
    String userAgent;

    @Column(name = "return_url", length = 1000)
    String returnUrl;

    @Column(name = "cancel_url", length = 1000)
    String cancelUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    @Column(name = "metadata", columnDefinition = "JSON")
    String metadata;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    // Relationships
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    VNPayPaymentDetails vnpayDetails;

    // Helper methods
    public boolean isSuccessful() {
        return status != null && status.isSuccess();
    }

    public boolean isPending() {
        return status != null && status.isPending();
    }

    public boolean isFailed() {
        return status != null && status.isFailed();
    }

    public boolean isVNPay() {
        return PaymentProvider.VNPAY.equals(provider);
    }

}
