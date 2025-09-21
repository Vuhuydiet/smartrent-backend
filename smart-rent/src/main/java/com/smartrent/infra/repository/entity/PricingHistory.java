package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "pricing_histories")
@Table(name = "pricing_histories",
        indexes = {
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_listing_date", columnList = "listing_id, changed_at"),
                @Index(name = "idx_changed_at", columnList = "changed_at"),
                @Index(name = "idx_is_current", columnList = "is_current")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    Listing listing;

    @Column(name = "old_price", precision = 15, scale = 0)
    BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 15, scale = 0)
    BigDecimal newPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_price_unit")
    Listing.PriceUnit oldPriceUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_price_unit", nullable = false)
    Listing.PriceUnit newPriceUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    PriceChangeType changeType;

    @Column(name = "change_percentage", precision = 5, scale = 2)
    BigDecimal changePercentage;

    @Column(name = "change_amount", precision = 15, scale = 0)
    BigDecimal changeAmount;

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    Boolean isCurrent = true;

    @Column(name = "changed_by")
    String changedBy; // User ID who made the change

    @Column(name = "change_reason", length = 500)
    String changeReason;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    LocalDateTime changedAt;

    // Enums
    public enum PriceChangeType {
        INITIAL, // First price when listing is created
        INCREASE, // Price went up
        DECREASE, // Price went down
        UNIT_CHANGE, // Price unit changed (e.g., from monthly to daily)
        CORRECTION // Manual correction/adjustment
    }

    // Helper methods
    public boolean isPriceIncrease() {
        return changeType == PriceChangeType.INCREASE;
    }

    public boolean isPriceDecrease() {
        return changeType == PriceChangeType.DECREASE;
    }

    public boolean isInitialPrice() {
        return changeType == PriceChangeType.INITIAL;
    }
}
