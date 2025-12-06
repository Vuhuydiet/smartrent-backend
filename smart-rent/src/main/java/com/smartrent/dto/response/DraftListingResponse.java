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
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Draft listing response")
public class DraftListingResponse {

    @Schema(description = "Draft ID")
    Long draftId;

    @Schema(description = "User ID")
    String userId;

    @Schema(description = "Listing title")
    String title;

    @Schema(description = "Listing description")
    String description;

    @Schema(description = "Listing type (RENT, SALE, SHARE)")
    String listingType;

    @Schema(description = "VIP type (NORMAL, SILVER, GOLD, DIAMOND)")
    String vipType;

    @Schema(description = "Category ID")
    Long categoryId;

    @Schema(description = "Product type (ROOM, APARTMENT, HOUSE, OFFICE, STUDIO)")
    String productType;

    @Schema(description = "Price")
    BigDecimal price;

    @Schema(description = "Price unit (MONTH, DAY, YEAR)")
    String priceUnit;

    // Address information
    @Schema(description = "Address type (OLD, NEW)")
    String addressType;

    @Schema(description = "Province ID (legacy)")
    Long provinceId;

    @Schema(description = "District ID (legacy)")
    Long districtId;

    @Schema(description = "Ward ID (legacy)")
    Long wardId;

    @Schema(description = "Province code (new)")
    String provinceCode;

    @Schema(description = "Ward code (new)")
    String wardCode;

    @Schema(description = "Street name")
    String street;

    @Schema(description = "Street ID")
    Long streetId;

    @Schema(description = "Project ID")
    Long projectId;

    @Schema(description = "Latitude")
    Double latitude;

    @Schema(description = "Longitude")
    Double longitude;

    // Property specifications
    @Schema(description = "Area in square meters")
    Float area;

    @Schema(description = "Number of bedrooms")
    Integer bedrooms;

    @Schema(description = "Number of bathrooms")
    Integer bathrooms;

    @Schema(description = "Direction")
    String direction;

    @Schema(description = "Furnishing level")
    String furnishing;

    @Schema(description = "Room capacity")
    Integer roomCapacity;

    // Utility costs
    @Schema(description = "Water price type")
    String waterPrice;

    @Schema(description = "Electricity price type")
    String electricityPrice;

    @Schema(description = "Internet price type")
    String internetPrice;

    @Schema(description = "Service fee")
    String serviceFee;

    // Related IDs
    @Schema(description = "Amenity IDs")
    Set<Long> amenityIds;

    @Schema(description = "Media IDs")
    Set<Long> mediaIds;

    // Timestamps
    @Schema(description = "Created at")
    LocalDateTime createdAt;

    @Schema(description = "Last updated at")
    LocalDateTime updatedAt;
}

