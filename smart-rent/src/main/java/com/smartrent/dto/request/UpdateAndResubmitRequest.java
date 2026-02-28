package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Request body for the combined "update listing content + resubmit for review" action.
 * Used by owners updating a REJECTED or REVISION_REQUIRED listing.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to update listing content and resubmit for review")
public class UpdateAndResubmitRequest {

    @Schema(description = "Updated listing title", example = "Căn hộ 2 phòng ngủ đã cập nhật")
    String title;

    @Schema(description = "Updated description", example = "Mô tả đã được chỉnh sửa theo yêu cầu")
    String description;

    @Schema(description = "Listing type", example = "RENT", allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "Category ID")
    Long categoryId;

    @Schema(description = "Product type", allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO"})
    String productType;

    @Schema(description = "Price")
    BigDecimal price;

    @Schema(description = "Price unit", allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    @Schema(description = "Area in square meters")
    Float area;

    @Schema(description = "Number of bedrooms")
    Integer bedrooms;

    @Schema(description = "Number of bathrooms")
    Integer bathrooms;

    @Schema(description = "Direction", allowableValues = {"NORTH", "SOUTH", "EAST", "WEST", "NORTHEAST", "NORTHWEST", "SOUTHEAST", "SOUTHWEST"})
    String direction;

    @Schema(description = "Furnishing level", allowableValues = {"FULLY_FURNISHED", "SEMI_FURNISHED", "UNFURNISHED"})
    String furnishing;

    @Schema(description = "Room capacity")
    Integer roomCapacity;

    @Schema(description = "Water price info")
    String waterPrice;

    @Schema(description = "Electricity price info")
    String electricityPrice;

    @Schema(description = "Internet price info")
    String internetPrice;

    @Schema(description = "Service fee info")
    String serviceFee;

    @Schema(description = "Amenity IDs to attach")
    Set<Long> amenityIds;

    @Schema(description = "Media IDs to attach (replaces existing if provided)")
    Set<Long> mediaIds;

    @Schema(description = "Optional notes describing what was changed",
            example = "Updated title and added missing photos as requested by admin")
    String notes;
}
