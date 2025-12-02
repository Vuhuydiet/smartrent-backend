package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Unified filter criteria for all listing search scenarios")
public class ListingFilterRequest {

    // ============ USER & OWNERSHIP FILTERS ============
    @Schema(description = "User ID - If provided, search listings of this user (my listings). If null, search all public listings", example = "user-123")
    String userId;

    @Schema(description = "Filter by draft status - true: only drafts, false: only published, null: both", example = "false")
    Boolean isDraft;

    @Schema(description = "Filter by verified status - true: only verified, false: only unverified, null: both", example = "true")
    Boolean verified;

    @Schema(description = "Filter by verification pending status - true: pending verification, false: not pending", example = "false")
    Boolean isVerify;

    @Schema(description = "Filter by expired status - true: only expired, false: only active, null: both", example = "false")
    Boolean expired;

    @Schema(description = "Exclude expired listings (default true for public search)", example = "true", defaultValue = "true")
    @Builder.Default
    Boolean excludeExpired = true;

    @Schema(description = "Filter by post status (ACTIVE, EXPIRED, PENDING, DRAFT, etc.)", example = "ACTIVE")
    String status;

    @Schema(description = "Filter by listing status for owner view - EXPIRED, EXPIRING_SOON, DISPLAYING, IN_REVIEW, PENDING_PAYMENT, REJECTED, VERIFIED",
            example = "DISPLAYING",
            allowableValues = {"EXPIRED", "EXPIRING_SOON", "DISPLAYING", "IN_REVIEW", "PENDING_PAYMENT", "REJECTED", "VERIFIED"})
    String listingStatus;

    // ============ LOCATION FILTERS ============
    @Schema(description = "Province ID (can be Integer or String for compatibility)", example = "1")
    String provinceId;

    @Schema(description = "Province code (new structure - 34 provinces)", example = "01")
    String provinceCode;

    @Schema(description = "District ID (old structure)", example = "5")
    Integer districtId;

    @Schema(description = "Ward ID (can be Integer or String for compatibility)", example = "123")
    String wardId;

    @Schema(description = "Ward code (new structure)", example = "00001")
    String newWardCode;

    @Schema(description = "Street ID", example = "10")
    Integer streetId;

    @Schema(description = "Use legacy address structure (true) or new structure (false)", example = "true")
    Boolean isLegacy;

    // Location coordinates (can be used for both user location and listing location)
    @Schema(description = "Latitude for location-based search", example = "21.0285")
    Double latitude;

    @Schema(description = "Longitude for location-based search", example = "105.8542")
    Double longitude;

    // User location-based search (kept for backward compatibility)
    @Schema(description = "User's current latitude for distance-based search", example = "21.0285")
    Double userLatitude;

    @Schema(description = "User's current longitude for distance-based search", example = "105.8542")
    Double userLongitude;

    @Schema(description = "Search radius in kilometers (requires userLatitude and userLongitude)", example = "5.0")
    Double radiusKm;

    // ============ CATEGORY & TYPE FILTERS ============
    @Schema(description = "Category ID to filter by", example = "1")
    Long categoryId;

    @Schema(description = "Listing type", example = "RENT", allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "VIP type", example = "SILVER", allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"})
    String vipType;

    @Schema(description = "Product type (loại bất động sản) - Filter by property category", example = "APARTMENT", allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO"})
    String productType;

    // ============ PROPERTY SPECS FILTERS ============
    // Basic price range filters
    @Schema(description = "Minimum price (in VND)")
    java.math.BigDecimal minPrice;

    @Schema(description = "Maximum price (in VND)")
    java.math.BigDecimal maxPrice;

    // Price unit filter
    @Schema(description = "Filter by price unit", example = "MONTH", allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    // Price change filters (based on pricing history)
    @Schema(description = "Only show listings with recent price reductions", example = "true")
    Boolean hasPriceReduction;

    @Schema(description = "Only show listings with recent price increases", example = "false")
    Boolean hasPriceIncrease;

    @Schema(description = "Minimum price reduction percentage (e.g., 10 for 10% off)", example = "10")
    java.math.BigDecimal minPriceReductionPercent;

    @Schema(description = "Maximum price reduction percentage (e.g., 50 for up to 50% off)", example = "50")
    java.math.BigDecimal maxPriceReductionPercent;

    @Schema(description = "Filter listings with price changes within last X days", example = "30")
    Integer priceChangedWithinDays;

    @Schema(description = "Minimum area in square meters")
    Float minArea;

    @Schema(description = "Maximum area in square meters")
    Float maxArea;

    @Schema(description = "Exact number of bedrooms (use minBedrooms/maxBedrooms for range)")
    Integer bedrooms;

    @Schema(description = "Exact number of bathrooms (use minBathrooms/maxBathrooms for range)")
    Integer bathrooms;

    @Schema(description = "Minimum number of bedrooms")
    Integer minBedrooms;

    @Schema(description = "Maximum number of bedrooms")
    Integer maxBedrooms;

    @Schema(description = "Minimum number of bathrooms")
    Integer minBathrooms;

    @Schema(description = "Maximum number of bathrooms")
    Integer maxBathrooms;

    @Schema(description = "Furnishing type", allowableValues = {"FULLY_FURNISHED", "SEMI_FURNISHED", "UNFURNISHED"})
    String furnishing;

    @Schema(description = "Direction", allowableValues = {"NORTH", "SOUTH", "EAST", "WEST", "NORTHEAST", "NORTHWEST", "SOUTHEAST", "SOUTHWEST"})
    String direction;

    @Schema(description = "Minimum room capacity")
    Integer minRoomCapacity;

    @Schema(description = "Maximum room capacity")
    Integer maxRoomCapacity;

    // ============ UTILITY PRICE FILTERS ============
    @Schema(description = "Water price filter (e.g., 'LOW', 'MEDIUM', 'HIGH', or specific range)", example = "LOW")
    String waterPrice;

    @Schema(description = "Electricity price filter (e.g., 'LOW', 'MEDIUM', 'HIGH', or specific range)", example = "MEDIUM")
    String electricityPrice;

    @Schema(description = "Internet price filter (e.g., 'FREE', 'LOW', 'MEDIUM', 'HIGH')", example = "FREE")
    String internetPrice;

    @Schema(description = "Service fee filter (e.g., 'LOW', 'MEDIUM', 'HIGH', or specific range)", example = "LOW")
    String serviceFee;

    // ============ AMENITIES & MEDIA FILTERS ============
    @Schema(description = "List of amenity IDs to filter by", example = "[1, 3, 5]")
    java.util.Set<Long> amenityIds;

    @Schema(description = "Amenity match mode - ALL: must have all amenities, ANY: must have at least one",
            allowableValues = {"ALL", "ANY"}, defaultValue = "ALL")
    @Builder.Default
    String amenityMatchMode = "ALL";

    @Schema(description = "Only show listings with media (photos/videos)", example = "true")
    Boolean hasMedia;

    @Schema(description = "Minimum number of media items")
    Integer minMediaCount;

    // ============ CONTENT SEARCH ============
    @Schema(description = "Keyword search in title and description", example = "căn hộ cao cấp view đẹp")
    String keyword;

    // ============ CONTACT FILTERS ============
    @Schema(description = "Only show listings with verified owner phone number", example = "true")
    Boolean ownerPhoneVerified;

    // ============ TIME FILTERS ============
    @Schema(description = "Show only listings posted within last X days", example = "7")
    Integer postedWithinDays;

    @Schema(description = "Show only listings updated within last X days", example = "3")
    Integer updatedWithinDays;

    // ============ PAGINATION & SORTING ============
    @Schema(description = "Page number (one-based)", example = "1", defaultValue = "1")
    @Builder.Default
    Integer page = 1;

    @Schema(description = "Page size (max 100)", example = "20", defaultValue = "20")
    @Builder.Default
    Integer size = 20;

    @Schema(description = "Sort by field", example = "postDate",
            allowableValues = {"DEFAULT", "PRICE_ASC", "PRICE_DESC", "NEWEST", "OLDEST"})
    String sortBy;

    @Schema(description = "Sort direction", example = "DESC", allowableValues = {"ASC", "DESC"})
    @Builder.Default
    String sortDirection = "DESC";
}
