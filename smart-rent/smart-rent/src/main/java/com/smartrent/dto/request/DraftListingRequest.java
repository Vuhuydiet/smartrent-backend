package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
 * Request DTO for creating draft listings.
 * All fields are optional to support auto-save functionality.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request for creating a draft listing. All fields are optional.")
public class DraftListingRequest {

    /**
     * User ID - automatically populated from authentication token
     * DO NOT include this field in the request body
     */
    @Schema(hidden = true)
    String userId;

    @Size(max = 200)
    @Schema(description = "Listing title", example = "Căn hộ 2 phòng ngủ")
    String title;

    @Schema(description = "Listing description", example = "Căn hộ đang cập nhật thông tin...")
    String description;

    @Schema(description = "Listing type", example = "RENT", allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "VIP tier type", example = "SILVER", allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"})
    String vipType;

    @Schema(description = "Category ID", example = "1")
    Long categoryId;

    @Schema(description = "Product type", example = "APARTMENT", allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO"})
    String productType;

    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(description = "Price", example = "12000000")
    BigDecimal price;

    @Schema(description = "Price unit", example = "MONTH", allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    @Valid
    @Schema(description = "Address information")
    AddressCreationRequest address;

    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(description = "Area in square meters", example = "78.5")
    Float area;

    @Min(0)
    @Schema(description = "Number of bedrooms", example = "2")
    Integer bedrooms;

    @Min(0)
    @Schema(description = "Number of bathrooms", example = "1")
    Integer bathrooms;

    @Schema(description = "Direction", example = "NORTHEAST")
    String direction;

    @Schema(description = "Furnishing level", example = "SEMI_FURNISHED")
    String furnishing;

    @Min(0)
    @Schema(description = "Room capacity", example = "4")
    Integer roomCapacity;

    @Schema(description = "List of amenity IDs")
    Set<Long> amenityIds;

    @Schema(description = "List of media IDs to attach to this listing")
    Set<Long> mediaIds;

    @Schema(description = "Water price type", example = "NEGOTIABLE")
    String waterPrice;

    @Schema(description = "Electricity price type", example = "SET_BY_OWNER")
    String electricityPrice;

    @Schema(description = "Internet price type", example = "PROVIDER_RATE")
    String internetPrice;

    @Schema(description = "Service fee")
    String serviceFee;
}

