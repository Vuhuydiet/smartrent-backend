package com.smartrent.infra.repository.entity;

import com.smartrent.enums.PostSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "listings")
@Table(name = "listings",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_category_type", columnList = "category_id, listing_type"),
                @Index(name = "idx_address", columnList = "address_id"),
                @Index(name = "idx_price_type", columnList = "price, listing_type"),
                @Index(name = "idx_status", columnList = "verified, expired, vip_type"),
                @Index(name = "idx_expiry_date", columnList = "expiry_date"),
                @Index(name = "idx_post_date", columnList = "post_date")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Listing {

    @Id
    @Column(name = "listing_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long listingId;

    // Core Information
    @Column(name = "title", nullable = false, length = 200)
    String title;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    String description;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "post_date")
    @CreationTimestamp
    LocalDateTime postDate;

    @Column(name = "expiry_date")
    LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    ListingType listingType;


    @Builder.Default
    @Column(name = "verified", nullable = false)
    Boolean verified = false;

    @Builder.Default
    @Column(name = "is_verify", nullable = false)
    Boolean isVerify = false;

    @Builder.Default
    @Column(name = "expired", nullable = false)
    Boolean expired = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "vip_type", nullable = false)
    VipType vipType = VipType.NORMAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "post_source")
    PostSource postSource = PostSource.QUOTA;

    @Column(name = "transaction_id", length = 36)
    String transactionId;

    @Builder.Default
    @Column(name = "is_shadow", nullable = false)
    Boolean isShadow = false;

    @Column(name = "parent_listing_id")
    Long parentListingId;

    @Column(name = "pushed_at")
    LocalDateTime pushedAt;

    @Column(name = "category_id", nullable = false)
    Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    ProductType productType;

    // Pricing Information
    @Column(name = "price", nullable = false, precision = 15, scale = 0)
    BigDecimal price;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", nullable = false)
    PriceUnit priceUnit = PriceUnit.MONTH;

    // Location Information - Reference to Address table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    Address address;

    // Property Specifications
    @Column(name = "area")
    Float area;

    @Column(name = "bedrooms")
    Integer bedrooms;

    @Column(name = "bathrooms")
    Integer bathrooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "furnishing")
    Furnishing furnishing;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    PropertyType propertyType;

    @Column(name = "room_capacity")
    Integer roomCapacity;

    // Relationships
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    List<Media> media;

    @ManyToMany
    @JoinTable(
            name = "listing_amenities",
            joinColumns = @JoinColumn(name = "listing_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    List<Amenity> amenities;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Favorite> favorites;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<View> views;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    List<PricingHistory> pricingHistories;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Column(name = "updated_by")
    Long updatedBy;

    // Enums
    public enum ListingType {
        RENT, SALE, SHARE
    }

    public enum VipType {
        NORMAL, SILVER, GOLD, DIAMOND
    }

    public enum ProductType {
        ROOM, APARTMENT, HOUSE, OFFICE, STUDIO
    }

    public enum PriceUnit {
        MONTH, DAY, YEAR
    }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST,
        NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST
    }

    public enum Furnishing {
        FULLY_FURNISHED, SEMI_FURNISHED, UNFURNISHED
    }

    public enum PropertyType {
        APARTMENT, HOUSE, ROOM, STUDIO, OFFICE
    }

    // Helper methods for VIP types
    public boolean isNormal() {
        return vipType == VipType.NORMAL;
    }

    public boolean isSilver() {
        return vipType == VipType.SILVER;
    }

    public boolean isGold() {
        return vipType == VipType.GOLD;
    }

    public boolean isDiamond() {
        return vipType == VipType.DIAMOND;
    }

    // Helper methods for shadow listings
    public boolean isShadowListing() {
        return isShadow != null && isShadow;
    }

    public boolean hasParentListing() {
        return parentListingId != null;
    }

    // Helper methods for verification
    public boolean isAutoVerified() {
        return isVerify != null && isVerify;
    }

    public boolean isManuallyVerified() {
        return verified != null && verified;
    }

    // Helper methods for post source
    public boolean isCreatedWithQuota() {
        return postSource == PostSource.QUOTA;
    }

    public boolean isCreatedWithDirectPayment() {
        return postSource == PostSource.DIRECT_PAYMENT;
    }

    public boolean hasLinkedTransaction() {
        return transactionId != null && !transactionId.isEmpty();
    }

    // Helper methods for expiry
    public boolean isExpiredListing() {
        return expired != null && expired;
    }

    public boolean isActive() {
        return !isExpiredListing() && (isAutoVerified() || isManuallyVerified());
    }
}