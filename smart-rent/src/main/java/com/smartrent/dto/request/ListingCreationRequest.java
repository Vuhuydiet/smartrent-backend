package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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

    /**
     * VIP tier type for the listing (NORMAL, SILVER, GOLD, DIAMOND)
     * Optional when useMembershipQuota=true with benefitIds - will be inferred from benefit type:
     * - POST_SILVER benefit -> SILVER vipType
     * - POST_GOLD benefit -> GOLD vipType
     * - POST_DIAMOND benefit -> DIAMOND vipType
     * Required when useMembershipQuota=false (payment flow)
     */
    @Schema(description = "VIP tier type. Optional when using membership quota (inferred from benefitIds)",
            example = "GOLD",
            allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"})
    String vipType;

    Long categoryId;

    @Schema(description = "Product type (loại bất động sản)", example = "APARTMENT", allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO"})
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

    @Min(0)
    Integer roomCapacity;

    Set<Long> amenityIds;

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
     * List of user membership benefit IDs to use for creating this listing
     * Required when useMembershipQuota is true
     * Each benefitId corresponds to a UserMembershipBenefit record
     * The system will consume quota from these specific benefits
     * All benefits must have the same type (e.g., all POST_GOLD)
     * The vipType will be automatically inferred from the benefit type
     */
    @Schema(description = "List of user membership benefit IDs to use when useMembershipQuota=true. VipType is inferred from benefit type.")
    Set<Long> benefitIds;

    /**
     * Payment provider to use (VNPAY, etc.)
     * Required when useMembershipQuota is false
     */
    String paymentProvider;
}