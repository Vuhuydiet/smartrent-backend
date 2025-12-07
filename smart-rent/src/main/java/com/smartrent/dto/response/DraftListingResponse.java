package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * Response DTO for draft listings.
 * Contains all draft data including partial information.
 * Draft listings allow users to save incomplete listings and publish them later.
 * All fields are optional to support auto-save functionality.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Draft listing response containing partial or complete listing information")
public class DraftListingResponse {

    @Schema(
        description = "Unique identifier for the draft listing",
        example = "123"
    )
    Long draftId;

    @Schema(
        description = "ID of the user who created this draft",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    String userId;

    @Schema(
        description = "Listing title",
        example = "Cho thuê căn hộ 2PN view đẹp Q7"
    )
    String title;

    @Schema(
        description = "Detailed description of the listing",
        example = "Căn hộ 2 phòng ngủ, nội thất đầy đủ, view sông thoáng mát"
    )
    String description;

    @Schema(
        description = "Type of listing",
        example = "RENT",
        allowableValues = {"RENT", "SALE", "SHARE"}
    )
    String listingType;

    @Schema(
        description = "VIP tier for listing visibility and features",
        example = "SILVER",
        allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"}
    )
    String vipType;

    @Schema(
        description = "Property category ID",
        example = "1"
    )
    Long categoryId;

    @Schema(
        description = "Type of property",
        example = "APARTMENT",
        allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO"}
    )
    String productType;

    @Schema(
        description = "Listing price",
        example = "5000000"
    )
    BigDecimal price;

    @Schema(
        description = "Price unit",
        example = "MONTH",
        allowableValues = {"MONTH", "DAY", "YEAR"}
    )
    String priceUnit;

    // Address information
    @Schema(
        description = "Full address information with nested legacy/new structure. " +
                      "Supports both OLD (63 provinces, 3-tier: province/district/ward) and " +
                      "NEW (34 provinces, 2-tier: province/ward) address formats. " +
                      "May be null if address not yet provided in draft.",
        implementation = AddressResponse.class
    )
    AddressResponse address;

    // Property specifications
    @Schema(
        description = "Area in square meters",
        example = "45.5"
    )
    Float area;

    @Schema(
        description = "Number of bedrooms",
        example = "2"
    )
    Integer bedrooms;

    @Schema(
        description = "Number of bathrooms",
        example = "1"
    )
    Integer bathrooms;

    @Schema(
        description = "Direction/orientation of the property",
        example = "EAST",
        allowableValues = {"NORTH", "SOUTH", "EAST", "WEST", "NORTHEAST", "NORTHWEST", "SOUTHEAST", "SOUTHWEST"}
    )
    String direction;

    @Schema(
        description = "Furnishing level of the property",
        example = "FULLY_FURNISHED",
        allowableValues = {"FULLY_FURNISHED", "SEMI_FURNISHED", "UNFURNISHED"}
    )
    String furnishing;

    @Schema(
        description = "Maximum number of people that can stay",
        example = "4"
    )
    Integer roomCapacity;

    // Utility costs
    @Schema(
        description = "Water price information (free text)",
        example = "50000 VND/month"
    )
    String waterPrice;

    @Schema(
        description = "Electricity price information (free text)",
        example = "3500 VND/kWh"
    )
    String electricityPrice;

    @Schema(
        description = "Internet price information (free text)",
        example = "100000 VND/month"
    )
    String internetPrice;

    @Schema(
        description = "Service fee information (free text)",
        example = "200000 VND/month"
    )
    String serviceFee;

    // Related entities
    @Schema(
        description = "Set of amenities/facilities available in the property (e.g., parking, elevator, security). " +
                      "May be empty if not yet selected in draft.",
        implementation = AmenityResponse.class
    )
    Set<AmenityResponse> amenities;

    @Schema(
        description = "Set of media (images/videos) attached to this draft. " +
                      "Media must be uploaded separately before being linked to draft. " +
                      "May be empty if no media uploaded yet.",
        implementation = MediaResponse.class
    )
    Set<MediaResponse> media;

    // Timestamps
    @Schema(
        description = "Timestamp when draft was created",
        example = "2024-12-06T10:30:00"
    )
    LocalDateTime createdAt;

    @Schema(
        description = "Timestamp when draft was last updated (auto-save or manual save)",
        example = "2024-12-06T14:45:00"
    )
    LocalDateTime updatedAt;
}

