package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "user_wallets")
@Table(name = "user_wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_wallets_user_id", columnNames = {"user_id"})
        },
        indexes = {
                @Index(name = "idx_user_wallets_user_id", columnList = "user_id"),
                @Index(name = "idx_user_wallets_credit_balance", columnList = "credit_balance"),
                @Index(name = "idx_user_wallets_currency", columnList = "currency"),
                @Index(name = "idx_user_wallets_is_active", columnList = "is_active"),
                @Index(name = "idx_user_wallets_created_at", columnList = "created_at"),
                @Index(name = "idx_user_wallets_updated_at", columnList = "updated_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    Long walletId;

    @Column(name = "user_id", nullable = false)
    String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;

    @Column(name = "credit_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal creditBalance = BigDecimal.ZERO;

    @Column(name = "total_credits_added", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalCreditsAdded = BigDecimal.ZERO;

    @Column(name = "total_credits_spent", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    BigDecimal totalCreditsSpent = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    String currency = "VND";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    // Helper methods
    public boolean hasSufficientBalance(BigDecimal amount) {
        return creditBalance != null && creditBalance.compareTo(amount) >= 0;
    }

    public void addCredit(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.creditBalance = this.creditBalance.add(amount);
            this.totalCreditsAdded = this.totalCreditsAdded.add(amount);
        }
    }

    public boolean subtractCredit(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0 && hasSufficientBalance(amount)) {
            this.creditBalance = this.creditBalance.subtract(amount);
            this.totalCreditsSpent = this.totalCreditsSpent.add(amount);
            return true;
        }
        return false;
    }

    public boolean isActiveWallet() {
        return isActive != null && isActive;
    }
}