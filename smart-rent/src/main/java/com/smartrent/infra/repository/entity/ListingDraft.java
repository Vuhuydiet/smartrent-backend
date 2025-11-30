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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for storing draft listings.
 * All fields are optional to support auto-save functionality.
 * When a draft is published, it will be converted to a Listing entity.
 */
@Entity
@Table(name = "listing_drafts",
        indexes = {
                @Index(name = "idx_draft_user_id", columnList = "user_id"),
                @Index(name = "idx_draft_updated_at", columnList = "updated_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingDraft {

    @Id
    @Column(name = "draft_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long draftId;

    // User who owns this draft - required
    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    // Core Information - all optional
    @Column(name = "title", length = 200)
    String title;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    String description;

    @Column(name = "listing_type", length = 20)
    String listingType;

    @Column(name = "vip_type", length = 20)
    String vipType;

    @Column(name = "category_id")
    Long categoryId;

    @Column(name = "product_type", length = 20)
    String productType;

    // Pricing Information - optional
    @Column(name = "price", precision = 15, scale = 0)
    BigDecimal price;

    @Column(name = "price_unit", length = 20)
    String priceUnit;

    // Address Information - stored as JSON or separate fields
    @Column(name = "address_type", length = 10)
    String addressType;

    // Legacy address fields
    @Column(name = "province_id")
    Long provinceId;

    @Column(name = "district_id")
    Long districtId;

    @Column(name = "ward_id")
    Long wardId;

    // New address fields
    @Column(name = "province_code", length = 20)
    String provinceCode;

    @Column(name = "ward_code", length = 20)
    String wardCode;

    // Common address fields
    @Column(name = "street", length = 255)
    String street;

    @Column(name = "street_id")
    Long streetId;

    @Column(name = "project_id")
    Long projectId;

    @Column(name = "latitude")
    Double latitude;

    @Column(name = "longitude")
    Double longitude;

    // Property Specifications - optional
    @Column(name = "area")
    Float area;

    @Column(name = "bedrooms")
    Integer bedrooms;

    @Column(name = "bathrooms")
    Integer bathrooms;

    @Column(name = "direction", length = 20)
    String direction;

    @Column(name = "furnishing", length = 30)
    String furnishing;

    @Column(name = "room_capacity")
    Integer roomCapacity;

    // Utility Costs - optional
    @Column(name = "water_price", length = 50)
    String waterPrice;

    @Column(name = "electricity_price", length = 50)
    String electricityPrice;

    @Column(name = "internet_price", length = 50)
    String internetPrice;

    @Column(name = "service_fee", length = 50)
    String serviceFee;

    // Amenity IDs stored as comma-separated string
    @Column(name = "amenity_ids", length = 500)
    String amenityIds;

    // Media IDs stored as comma-separated string
    @Column(name = "media_ids", length = 500)
    String mediaIds;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;
}

