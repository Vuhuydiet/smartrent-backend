package com.smartrent.dto.request;

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
 * Request for updating draft listing
 * All fields are optional - allows partial updates
 * No validation for required fields (validation happens on publish)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to update draft listing - All fields are optional")
public class ListingDraftUpdateRequest {

    @Schema(description = "Listing title", example = "Căn hộ 2 phòng ngủ view đẹp")
    String title;

    @Schema(description = "Detailed description")
    String description;

    @Schema(description = "Expiry date")
    LocalDateTime expiryDate;

    @Schema(description = "Listing type", example = "RENT", allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "VIP type", example = "NORMAL", allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"})
    String vipType;

    @Schema(description = "Category ID", example = "1")
    Long categoryId;

    @Schema(description = "Product type (loại bất động sản)", example = "APARTMENT", allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO"})
    String productType;

    @Schema(description = "Price in VND", example = "5000000")
    BigDecimal price;

    @Schema(description = "Price unit", example = "MONTH", allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    // Address fields
    @Schema(description = "Full address string", example = "123 Nguyễn Huệ, Quận 1, TP.HCM")
    String fullAddress;

    @Schema(description = "Province ID (legacy)", example = "79")
    Integer legacyProvinceId;

    @Schema(description = "District ID (legacy)", example = "760")
    Integer legacyDistrictId;

    @Schema(description = "Ward ID (legacy)", example = "26734")
    Integer legacyWardId;

    @Schema(description = "Street name", example = "Nguyễn Huệ")
    String legacyStreet;

    @Schema(description = "New province code", example = "79")
    String newProvinceCode;

    @Schema(description = "New ward code", example = "26734")
    String newWardCode;

    @Schema(description = "New street name", example = "Nguyễn Huệ")
    String newStreet;

    @Schema(description = "Latitude", example = "10.7769")
    Double latitude;

    @Schema(description = "Longitude", example = "106.7009")
    Double longitude;

    @Schema(description = "Address type", example = "NEW", allowableValues = {"OLD", "NEW"})
    String addressType;

    // Property specifications
    @Schema(description = "Area in square meters", example = "75.5")
    Float area;

    @Schema(description = "Number of bedrooms", example = "2")
    Integer bedrooms;

    @Schema(description = "Number of bathrooms", example = "1")
    Integer bathrooms;

    @Schema(description = "Direction", allowableValues = {"NORTH", "SOUTH", "EAST", "WEST", "NORTHEAST", "NORTHWEST", "SOUTHEAST", "SOUTHWEST"})
    String direction;

    @Schema(description = "Furnishing status", allowableValues = {"FULLY_FURNISHED", "SEMI_FURNISHED", "UNFURNISHED"})
    String furnishing;

    @Schema(description = "Room capacity", example = "4")
    Integer roomCapacity;

    // Utility prices
    @Schema(description = "Water price information", example = "20.000đ/khối")
    String waterPrice;

    @Schema(description = "Electricity price information", example = "3.500đ/kwh")
    String electricityPrice;

    @Schema(description = "Internet price information", example = "100.000đ/tháng")
    String internetPrice;

    @Schema(description = "Service fee information", example = "200.000đ/tháng")
    String serviceFee;

    // Amenities
    @Schema(description = "Set of amenity IDs", example = "[1, 3, 5]")
    Set<Long> amenityIds;

    @Schema(description = "Duration in days for the listing", example = "30")
    Integer durationDays;

    @Schema(description = "Payment provider if paid", example = "VNPAY")
    String paymentProvider;

    @Schema(description = "JSON string containing additional draft data")
    String draftData;
}
