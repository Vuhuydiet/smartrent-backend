package com.smartrent.dto.request;

import com.smartrent.infra.repository.entity.Listing;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to create a VIP listing with quota or direct payment")
public class VipListingCreationRequest {

    @Schema(description = "User ID (set by controller from header)", hidden = true)
    String userId;

    @NotBlank(message = "Title is required")
    @Schema(description = "Listing title", example = "Cho thuê căn hộ 2PN Q7", required = true)
    String title;

    @Schema(description = "Detailed description of the listing", example = "Căn hộ đẹp, view sông, đầy đủ nội thất")
    String description;

    @NotNull(message = "Listing type is required")
    @Schema(description = "Type of listing", example = "RENT", allowableValues = {"RENT", "SALE", "SHARE"}, required = true)
    String listingType;

    @NotNull(message = "VIP type is required")
    @Schema(
        description = "VIP tier for the listing. SILVER (50k/day), GOLD (110k/day), DIAMOND (280k/day)",
        example = "SILVER",
        allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"},
        required = true
    )
    String vipType;

    @NotNull(message = "Category ID is required")
    @Schema(description = "Category ID", example = "1", required = true)
    Long categoryId;

    @NotNull(message = "Product type is required")
    @Schema(description = "Product type", example = "APARTMENT", allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO"}, required = true)
    String productType;

    @NotNull(message = "Price is required")
    @Schema(description = "Rental/sale price", example = "15000000", required = true)
    BigDecimal price;

    @Schema(description = "Price unit", example = "MONTH", allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    @Valid
    @NotNull(message = "Address information is required")
    @Schema(description = "Address information for creating new address with this listing", required = true)
    AddressCreationRequest address;

    @Schema(description = "Area in square meters", example = "75.5")
    Float area;

    @Schema(description = "Number of bedrooms", example = "2")
    Integer bedrooms;

    @Schema(description = "Number of bathrooms", example = "2")
    Integer bathrooms;

    @Schema(description = "Direction", example = "EAST")
    String direction;

    @Schema(description = "Furnishing status", example = "FULLY_FURNISHED")
    String furnishing;

    @Schema(description = "Property type", example = "APARTMENT")
    String propertyType;

    @Schema(description = "Room capacity", example = "4")
    Integer roomCapacity;

    @Schema(description = "Set of amenity IDs", example = "[1, 2, 3]")
    Set<Long> amenityIds;

    @Schema(
        description = "Set of media IDs to attach to this listing (media must be already uploaded and belong to the same user)",
        example = "[1, 2, 3]"
    )
    Set<Long> mediaIds;

    @Schema(
        description = "Whether to use membership quota (true) or direct payment (false)",
        example = "true"
    )
    Boolean useMembershipQuota;

    @Schema(
        description = "Duration in days (10, 15, or 30). Longer durations get discounts: 15 days (11% off), 30 days (18.5% off). DEPRECATED: Use durationPlanId instead",
        example = "30",
        allowableValues = {"10", "15", "30"},
        deprecated = true
    )
    Integer durationDays;

    @Schema(
        description = "Duration plan ID (alternative to durationDays). Plans: 5d, 7d, 10d, 15d, 30d with automatic discount calculation",
        example = "5"
    )
    Long durationPlanId;

    @Schema(description = "Payment provider (only needed if useMembershipQuota = false)", example = "VNPAY")
    String paymentProvider;
}

