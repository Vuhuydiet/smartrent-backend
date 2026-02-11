package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "vip_tier_details")
@Table(name = "vip_tier_details",
        indexes = {
                @Index(name = "idx_tier_code", columnList = "tier_code"),
                @Index(name = "idx_tier_level", columnList = "tier_level"),
                @Index(name = "idx_is_active", columnList = "is_active")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VipTierDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    Long tierId;

    @Column(name = "tier_code", nullable = false, unique = true, length = 20)
    String tierCode;

    @Column(name = "tier_name", nullable = false, length = 100)
    String tierName;

    @Column(name = "tier_name_en", nullable = false, length = 100)
    String tierNameEn;

    @Column(name = "tier_level", nullable = false)
    Integer tierLevel;

    // Pricing
    @Column(name = "price_per_day", nullable = false, precision = 15, scale = 0)
    BigDecimal pricePerDay;

    @Column(name = "price_10_days", nullable = false, precision = 15, scale = 0)
    BigDecimal price10Days;

    @Column(name = "price_15_days", nullable = false, precision = 15, scale = 0)
    BigDecimal price15Days;

    @Column(name = "price_30_days", nullable = false, precision = 15, scale = 0)
    BigDecimal price30Days;

    // Features
    @Column(name = "max_images", nullable = false)
    Integer maxImages;

    @Column(name = "max_videos", nullable = false)
    Integer maxVideos;

    @Column(name = "has_badge", nullable = false)
    Boolean hasBadge;

    @Column(name = "badge_name", length = 100)
    String badgeName;

    @Column(name = "badge_color", length = 50)
    String badgeColor;

    @Column(name = "auto_approve", nullable = false)
    Boolean autoApprove;

    @Column(name = "no_ads", nullable = false)
    Boolean noAds;

    @Column(name = "priority_display", nullable = false)
    Boolean priorityDisplay;

    @Column(name = "has_shadow_listing", nullable = false)
    Boolean hasShadowListing;

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
    public boolean isNormal() {
        return "NORMAL".equalsIgnoreCase(tierCode);
    }

    public boolean isSilver() {
        return "SILVER".equalsIgnoreCase(tierCode);
    }

    public boolean isGold() {
        return "GOLD".equalsIgnoreCase(tierCode);
    }

    public boolean isDiamond() {
        return "DIAMOND".equalsIgnoreCase(tierCode);
    }

    public BigDecimal getPriceForDuration(int days) {
        return switch (days) {
            case 10 -> price10Days;
            case 15 -> price15Days;
            case 30 -> price30Days;
            default -> pricePerDay.multiply(BigDecimal.valueOf(days));
        };
    }
}

