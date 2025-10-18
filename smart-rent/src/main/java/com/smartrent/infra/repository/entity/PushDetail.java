package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "push_details")
@Table(name = "push_details",
        indexes = {
                @Index(name = "idx_detail_code", columnList = "detail_code"),
                @Index(name = "idx_is_active", columnList = "is_active")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_detail_id")
    Long pushDetailId;

    @Column(name = "detail_code", nullable = false, unique = true, length = 20)
    String detailCode;

    @Column(name = "detail_name", nullable = false, length = 100)
    String detailName;

    @Column(name = "detail_name_en", nullable = false, length = 100)
    String detailNameEn;

    // Pricing
    @Column(name = "price_per_push", nullable = false, precision = 15, scale = 0)
    BigDecimal pricePerPush;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 0)
    BigDecimal totalPrice;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    BigDecimal discountPercentage;

    // Description
    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "features", columnDefinition = "JSON")
    String features;

    // Status
    @Column(name = "is_active", nullable = false)
    Boolean isActive;

    @Column(name = "display_order", nullable = false)
    Integer displayOrder;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Helper methods
    public boolean isSinglePush() {
        return "SINGLE_PUSH".equalsIgnoreCase(detailCode);
    }

    public boolean isPackage() {
        return quantity > 1;
    }

    public BigDecimal calculateSavings() {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal originalPrice = pricePerPush.multiply(BigDecimal.valueOf(quantity));
        return originalPrice.subtract(totalPrice);
    }
}

