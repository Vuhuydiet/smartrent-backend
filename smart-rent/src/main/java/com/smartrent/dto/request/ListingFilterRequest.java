package com.smartrent.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

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

    @Schema(description = "Internal flag to indicate this is an admin request (set by backend, not by frontend)", hidden = true)
    @JsonIgnore
    Boolean isAdminRequest;

    @Schema(description = "Internal flag: userId was set by the backend from an authenticated principal "
            + "(owner's own dashboard), not supplied by an anonymous caller. Only requests with this flag "
            + "skip the isDraft=false / moderationStatus=APPROVED public-visibility gate. Set by backend, "
            + "not by frontend.", hidden = true)
    @JsonIgnore
    Boolean isOwnerRequest;

    @Schema(description = "Filter by moderation status (admin only)",
            example = "PENDING_REVIEW",
            allowableValues = {"PENDING_REVIEW", "APPROVED", "REJECTED", "REVISION_REQUIRED", "RESUBMITTED", "SUSPENDED"})
    String moderationStatus;

    @Schema(description = "Admin only. Only meaningful alongside moderationStatus=SUSPENDED, which is shared by "
            + "two unrelated admin actions: rejecting a listing in the review queue (always creates a pending "
            + "owner action) and temporarily hiding a listing under report review (never does). true narrows to "
            + "the reject case, false narrows to the hide case; omit to match both.",
            example = "true")
    Boolean hasPendingOwnerAction;

    // ============ LOCATION FILTERS ============
    @Schema(description = """
            Province ID (OLD structure - 63 provinces).
            Use this for legacy addresses. Example: '1' for Hanoi.
            For NEW structure (34 provinces), use provinceCode instead.
            """, example = "1")
    String provinceId;

    @Schema(description = """
            Province code (NEW structure - 34 provinces as of July 2025). Single code.
            Accepts both '1' and '01' formats (will be normalized).
            Examples: '1' or '01' for Hanoi, '79' for HCMC.
            """, example = "1")
    String provinceCode;

    @Schema(description = """
            List of Province codes (NEW structure - 34 provinces as of July 2025).
            Accepts both '1' and '01' formats (will be normalized).
            Examples: '1' or '01' for Hanoi, '79' for HCMC.
            """, example = "[\"1\", \"79\"]")
    List<String> provinceCodes;

    @JsonIgnore
    @Schema(hidden = true)
    List<Integer> resolvedLegacyProvinceIds; // Populated by service layer — NOT from API

    @JsonIgnore
    @Schema(hidden = true)
    List<String> resolvedNewProvinceCodes; // Populated by service layer when legacy provinceId is sent — NOT from API

    @Schema(description = "District ID (OLD structure) — the legacy_districts surrogate PK, "
            + "NOT the GSO administrative code. Emitted by the FE district dropdown.", example = "5")
    Integer districtId;

    @Schema(description = """
            District GSO administrative code (e.g. '760' = Quận 1, '765' = Bình Thạnh).
            Use this when you only know the official district code, not the internal
            legacy_districts PK. The service resolves it to districtId before filtering.
            """, example = "760")
    String districtCode;

    @JsonIgnore
    @Schema(hidden = true)
    List<String> resolvedNewWardCodesForDistrict; // Populated by service layer when legacy districtId is sent — NOT from API

    @Schema(description = """
            Ward ID (OLD structure - 63 provinces).
            Use this for legacy addresses.
            For NEW structure, use newWardCode instead.
            """, example = "123")
    String wardId;

    @Schema(description = """
            Ward code (NEW structure - 34 provinces).
            5-digit code for wards in the new administrative structure.
            """, example = "00001")
    String newWardCode;

    @JsonIgnore
    @Schema(hidden = true)
    List<Integer> resolvedLegacyWardIds; // Populated by service layer — NOT from API

    @JsonIgnore
    @Schema(hidden = true)
    List<String> resolvedNewWardCodes; // Populated by service layer when legacy wardId is sent — NOT from API

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
    // Range filters use a single string field with `..` separator. Format: "from..to".
    // Either side may be omitted for an open-ended range, e.g. "5000000..", "..15000000".
    @Schema(description = """
            Price range (VND). Format: `from..to`. Either side may be omitted.
            Examples: `5000000..15000000`, `5000000..` (≥ 5M), `..15000000` (≤ 15M).
            """, example = "5000000..15000000")
    String price;

    // Price unit filter
    @Schema(description = "Filter by price unit", example = "MONTH", allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    // Price change filters (based on pricing history)
    @Schema(description = "Only show listings with recent price reductions", example = "true")
    Boolean hasPriceReduction;

    @Schema(description = "Only show listings with recent price increases", example = "false")
    Boolean hasPriceIncrease;

    @Schema(description = """
            Price reduction percentage range. Format: `from..to`. Either side optional.
            Examples: `10..50` (10–50% off), `10..` (≥ 10%), `..50` (≤ 50%).
            """, example = "10..50")
    String priceReductionPercent;

    @Schema(description = "Filter listings with price changes within last X days", example = "30")
    Integer priceChangedWithinDays;

    @Schema(description = """
            Area range in square meters. Format: `from..to`. Either side optional.
            Examples: `30..60`, `30..`, `..60`.
            """, example = "30..60")
    String area;

    @Schema(description = "Exact number of bedrooms (use `bedroomsRange` for a range)")
    Integer bedrooms;

    @Schema(description = "Exact number of bathrooms (use `bathroomsRange` for a range)")
    Integer bathrooms;

    @Schema(description = """
            Bedrooms range. Format: `from..to`. Either side optional.
            Ignored when `bedrooms` (exact) is set.
            Examples: `2..4`, `2..`, `..4`.
            """, example = "2..4")
    String bedroomsRange;

    @Schema(description = """
            Bathrooms range. Format: `from..to`. Either side optional.
            Ignored when `bathrooms` (exact) is set.
            Examples: `1..3`, `1..`, `..3`.
            """, example = "1..3")
    String bathroomsRange;

    @Schema(description = "Furnishing type", allowableValues = {"FULLY_FURNISHED", "SEMI_FURNISHED", "UNFURNISHED"})
    String furnishing;

    @Schema(description = "Direction", allowableValues = {"NORTH", "SOUTH", "EAST", "WEST", "NORTHEAST", "NORTHWEST", "SOUTHEAST", "SOUTHWEST"})
    String direction;

    @Schema(description = """
            Room capacity range. Format: `from..to`. Either side optional.
            Examples: `2..6`, `2..`, `..6`.
            """, example = "2..6")
    String roomCapacity;

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
    @Schema(description = "Keyword search in title and description (FULLTEXT)", example = "căn hộ cao cấp view đẹp")
    String keyword;

    @Schema(description = "Case-insensitive substring search on listing TITLE only (admin table view)", example = "Tân Bình")
    String title;

    @Schema(description = """
            Admin-only search across owner first name, last name, and phone numbers
            (matches contactPhoneNumber OR phoneNumber). Case-insensitive substring match.
            Useful for finding all listings posted by a given user from the admin list.
            """, example = "0367919024")
    String ownerSearch;

    // ============ CONTACT FILTERS ============
    @Schema(description = "Only show listings with verified owner phone number", example = "true")
    Boolean ownerPhoneVerified;

    @Schema(description = "Filter by broker status – true: only listings from approved brokers, false: only non-broker owners, null: no filter", example = "true")
    Boolean isBroker;

    // ============ TIME FILTERS ============
    @Schema(description = "Show only listings posted within last X days", example = "7")
    Integer postedWithinDays;

    @Schema(description = "Show only listings updated within last X days", example = "3")
    Integer updatedWithinDays;

    @Schema(description = """
            Post date range — single field using `..` as separator. Format: `YYYY-MM-DD..YYYY-MM-DD`.
            Inclusive on both ends. Either side may be omitted for an open-ended range:
            - `2026-03-01..2026-03-31` — posted within March 2026
            - `2026-03-01..` — posted on or after Mar 1
            - `..2026-03-31` — posted on or before Mar 31
            Server pads the lower bound to start-of-day and the upper bound to end-of-day.
            """, example = "2026-03-01..2026-03-31")
    String postDate;

    @Schema(description = """
            Expiry date range — same `..` format as `postDate`.
            Examples: `2026-08-01..2026-08-31`, `2026-08-01..`, `..2026-08-31`.
            """, example = "2026-08-01..2026-08-31")
    String expiryDate;

    // ============ PAGINATION & SORTING ============
    @Schema(description = "Page number (one-based)", example = "1", defaultValue = "1")
    @Min(value = 1, message = "page must be >= 1")
    @Builder.Default
    Integer page = 1;

    @Schema(description = "Page size (max 100)", example = "20", defaultValue = "20")
    @Min(value = 1, message = "size must be >= 1")
    @Max(value = 100, message = "size must be <= 100")
    @Builder.Default
    Integer size = 20;

    @Schema(description = "Sort by field", example = "postDate",
            allowableValues = {"DEFAULT", "PRICE_ASC", "PRICE_DESC", "NEWEST", "OLDEST"})
    String sortBy;

    @Schema(description = "Sort direction", example = "DESC", allowableValues = {"ASC", "DESC"})
    @Builder.Default
    String sortDirection = "DESC";
}
