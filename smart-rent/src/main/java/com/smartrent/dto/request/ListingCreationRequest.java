package com.smartrent.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingCreationRequest {

    /**
     * User ID - automatically populated from authentication token
     * DO NOT include this field in the request body
     */
    String userId;

    // Fields that can be optional for draft listings
    @Size(max = 200)
    String title;

    String description;

    LocalDateTime expiryDate;

    String listingType;

    Boolean verified;
    Boolean isVerify;
    Boolean expired;

    String vipType;

    Long categoryId;

    String productType;

    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal price;

    String priceUnit;

    @Valid
    AddressCreationRequest address;

    @DecimalMin(value = "0.0", inclusive = false)
    Float area;

    @Min(0)
    Integer bedrooms;

    @Min(0)
    Integer bathrooms;

    String direction;

    String furnishing;

    String propertyType;

    @Min(0)
    Integer roomCapacity;

    Set<Long> amenityIds;

    /**
     * Whether this is a draft listing
     * If true, validation is relaxed to allow incomplete data
     * Default: false
     */
    @Builder.Default
    Boolean isDraft = false;

    LocalDateTime postedAt;

    String waterPrice;
    String electricityPrice;
    String internetPrice;
    String serviceFee;

    /**
     * List of media IDs to attach to this listing
     * Media must be already uploaded and belong to the same user
     * Optional: If not provided, listing will be created without media
     */
    Set<Long> mediaIds;

    /**
     * Duration in days for the listing (e.g., 10, 15, 30)
     * Required for NORMAL listings when creating through payment flow
     * The price will be calculated based on the VIP tier and duration
     */
    @Builder.Default
    Integer durationDays=30;

    /**
     * Whether to use membership quota (only applicable for VIP listings)
     * For NORMAL listings, this should always be false as they don't have quota
     * Default: false
     */
    @Builder.Default
    Boolean useMembershipQuota = false;

    /**
     * Payment provider to use (VNPAY, etc.)
     * Required when useMembershipQuota is false
     */
    String paymentProvider;
}