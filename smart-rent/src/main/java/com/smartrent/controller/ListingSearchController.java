package com.smartrent.controller;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.MapBoundsRequest;
import com.smartrent.dto.response.ListingCardListResponse;
import com.smartrent.dto.response.*;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.discovery.SearchSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Listings - Search & Discovery",
    description = """
        Public search, autocomplete, and map-based listing discovery.

        **Endpoints:**
        - `POST /search` - Full-featured search with 30+ filters, pagination, and sorting
        - `GET /autocomplete` - Lightweight title-prefix suggestions
        - `POST /map-bounds` - Geospatial search for interactive map display

        **Public API** - All endpoints in this group do NOT require authentication.
        Drafts, shadow listings, and expired listings are automatically excluded from results.
        """
)
@RequiredArgsConstructor
public class ListingSearchController {

    private final ListingService listingService;
    private final SearchSuggestionService searchSuggestionService;

    @PostMapping("/search")
    @Operation(
        summary = "[PUBLIC API] Tìm kiếm và lọc bài đăng - API tổng hợp cho màn hình chính",
        description = """
            **PUBLIC API - Không cần authentication**

            API tìm kiếm và lọc bài đăng tổng hợp tất cả các bộ lọc trong một lần gọi. Frontend chỉ cần gọi API này cho tất cả tính năng lọc ở màn hình chính.

            ## CÁC BỘ LỌC HỖ TRỢ

            ### 1. Lọc theo vị trí
            - `provinceId` (String - có thể là 63 tỉnh cũ hoặc 34 tỉnh mới) hoặc `provinceCode` (String - 34 tỉnh mới)
            - `districtId`, `wardId` (String - old) hoặc `newWardCode` (new), `streetId`
            - `isLegacy`: true (dùng cấu trúc 63 tỉnh cũ), false (dùng cấu trúc 34 tỉnh mới)
            - Tọa độ: `latitude`, `longitude` (dùng chung cho cả user location và listing location)
            - Tìm theo bán kính GPS: `userLatitude`, `userLongitude`, `radiusKm` (backward compatibility)

            ### 2. Lọc theo giá và diện tích
            - Khoảng giá: `minPrice`, `maxPrice` (VNĐ)
            - Đơn vị giá: `priceUnit` (MONTH, DAY, YEAR)
            - Giảm giá: `hasPriceReduction`, `minPriceReductionPercent`, `maxPriceReductionPercent`, `priceChangedWithinDays`
            - Diện tích: `minArea`, `maxArea` (m²)

            ### 3. Lọc theo đặc điểm nhà
            - Phòng: `minBedrooms`, `maxBedrooms`, `minBathrooms`, `maxBathrooms`
            - Nội thất: `furnishing` (FULLY_FURNISHED, SEMI_FURNISHED, UNFURNISHED)
            - Hướng: `direction` (NORTH, SOUTH, EAST, WEST, NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST)
            - Loại: `productType` (APARTMENT, HOUSE, ROOM, STUDIO, OFFICE)
            - Sức chứa: `minRoomCapacity`, `maxRoomCapacity`

            ### 4. Lọc theo chi phí tiện ích
            - `waterPrice`: giá nước (LOW, MEDIUM, HIGH)
            - `electricityPrice`: giá điện (LOW, MEDIUM, HIGH)
            - `internetPrice`: giá internet (FREE, LOW, MEDIUM, HIGH)
            - `serviceFee`: phí dịch vụ (LOW, MEDIUM, HIGH)

            ### 5. Lọc theo loại giao dịch, VIP và trạng thái
            - `listingType`: RENT, SALE, SHARE
            - `vipType`: NORMAL, SILVER, GOLD, DIAMOND
            - `verified`: true (chỉ lấy tin đã verify)
            - `status`: ACTIVE, EXPIRED, PENDING, DRAFT (trạng thái bài đăng)
            - `listingStatus`: EXPIRED, EXPIRING_SOON, DISPLAYING, IN_REVIEW, PENDING_PAYMENT, REJECTED, VERIFIED

            ### 6. Lọc theo tiện ích
            - `amenityIds`: array ID tiện ích (VD: [1, 3, 5])
            - `amenityMatchMode`: ALL (phải có tất cả), ANY (có ít nhất 1)

            ### 7. Lọc theo media
            - `hasMedia`: true (chỉ bài có ảnh/video)
            - `minMediaCount`: số lượng ảnh tối thiểu

            ### 8. Tìm kiếm từ khóa
            - `keyword`: tìm trong title và description

            ### 9. Lọc theo liên hệ
            - `ownerPhoneVerified`: true (chủ nhà đã xác thực SĐT)

            ### 10. Lọc theo thời gian
            - `postedWithinDays`: đăng trong X ngày
            - `updatedWithinDays`: cập nhật trong X ngày

            ### 11. Phân trang và sắp xếp
            - `page`: số trang (bắt đầu từ 0)
            - `size`: kích thước trang (mặc định 20, tối đa 100)
            - `sortBy`: DEFAULT (VIP tier + postDate), PRICE_ASC (giá tăng dần), PRICE_DESC (giá giảm dần), NEWEST (mới nhất), OLDEST (cũ nhất)
            - `sortDirection`: ASC, DESC (mặc định DESC)
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Request Example - Đầy đủ tất cả filter",
                        summary = "Tham khảo tất cả các filter có thể sử dụng (Frontend chỉ cần gửi các field cần thiết)",
                        value = """
                            {
                              "provinceId": "1",
                              "isLegacy": true,
                              "listingType": "RENT",
                              "productType": "APARTMENT",
                              "minPrice": 5000000,
                              "maxPrice": 15000000,
                              "priceUnit": "MONTH",
                              "minBedrooms": 2,
                              "maxBedrooms": 3,
                              "verified": true,
                              "hasMedia": true,
                              "page": 0,
                              "size": 20,
                              "sortBy": "DEFAULT",
                              "sortDirection": "DESC"
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results with pagination",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
                )
            )
        }
    )
    public ApiResponse<ListingCardListResponse> searchListings(
            @RequestBody(required = false) ListingFilterRequest filter,
            Authentication authentication) {

        if (filter == null) {
            filter = ListingFilterRequest.builder().build();
        }

        if ((filter.getUserId() == null || filter.getUserId().isEmpty()) &&
            authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getName())) {

            if (filter.getIsDraft() != null || filter.getIsVerify() != null) {
                filter.setUserId(authentication.getName());
            }
        }

        ListingCardListResponse response = listingService.searchListings(filter);
        return ApiResponse.<ListingCardListResponse>builder().data(response).build();
    }

    @GetMapping("/sellers/{userId}/diamond")
    @Operation(
        summary = "[PUBLIC API] Seller DIAMOND listings",
        description = "Get paginated DIAMOND listings for a public seller profile section."
    )
    public ApiResponse<ListingCardListResponse> getSellerDiamondListings(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "NEWEST") String sortBy) {
        return searchSellerListingsByVipType(userId, "DIAMOND", page, size, sortBy);
    }

    @GetMapping("/sellers/{userId}/gold")
    @Operation(
        summary = "[PUBLIC API] Seller GOLD listings",
        description = "Get paginated GOLD listings for a public seller profile section."
    )
    public ApiResponse<ListingCardListResponse> getSellerGoldListings(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "NEWEST") String sortBy) {
        return searchSellerListingsByVipType(userId, "GOLD", page, size, sortBy);
    }

    @GetMapping("/sellers/{userId}/silver")
    @Operation(
        summary = "[PUBLIC API] Seller SILVER listings",
        description = "Get paginated SILVER listings for a public seller profile section."
    )
    public ApiResponse<ListingCardListResponse> getSellerSilverListings(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "NEWEST") String sortBy) {
        return searchSellerListingsByVipType(userId, "SILVER", page, size, sortBy);
    }

    @GetMapping("/sellers/{userId}/normal")
    @Operation(
        summary = "[PUBLIC API] Seller NORMAL listings",
        description = "Get paginated NORMAL listings for a public seller profile section."
    )
    public ApiResponse<ListingCardListResponse> getSellerNormalListings(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "NEWEST") String sortBy) {
        return searchSellerListingsByVipType(userId, "NORMAL", page, size, sortBy);
    }

    @GetMapping("/sellers/{userId}/top-saved")
    @Operation(
        summary = "[PUBLIC API] Seller top saved listings",
        description = "Get top listings with most saves for a public seller profile."
    )
    public ApiResponse<ListingCardListResponse> getSellerTopSavedListings(
            @PathVariable String userId,
            @RequestParam(defaultValue = "5") Integer limit) {
        int safeLimit = limit != null && limit > 0 ? Math.min(limit, 20) : 5;
        ListingCardListResponse response = listingService.getTopSavedListingsByUser(userId, safeLimit);
        return ApiResponse.<ListingCardListResponse>builder().data(response).build();
    }

    private ApiResponse<ListingCardListResponse> searchSellerListingsByVipType(
            String userId,
            String vipType,
            Integer page,
            Integer size,
            String sortBy) {
        ListingFilterRequest filter = ListingFilterRequest.builder()
                .userId(userId)
                .vipType(vipType)
                .page(page != null && page > 0 ? page : 1)
                .size(size != null && size > 0 ? Math.min(size, 100) : 12)
                .sortBy(sortBy)
                .build();

        ListingCardListResponse response = listingService.searchListings(filter);
        return ApiResponse.<ListingCardListResponse>builder().data(response).build();
    }

    @GetMapping("/autocomplete")
    @Operation(
        summary = "Autocomplete listing titles",
        description = "Returns lightweight listing suggestions by title prefix."
    )
    public com.smartrent.dto.response.ApiResponse<List<ListingAutocompleteResponse>> autocompleteListings(
            @Parameter(description = "Query prefix", required = true, example = "can ho")
            @RequestParam("q") String query,
            @Parameter(description = "Max results (1-20)", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<ListingAutocompleteResponse> results = listingService.autocompleteListings(query, limit);
        return com.smartrent.dto.response.ApiResponse.<List<ListingAutocompleteResponse>>builder().data(results).build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /v1/listings/search-suggestions
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/search-suggestions")
    @Operation(
        summary = "[PUBLIC API] Smart multi-source search suggestions",
        description = """
            **PUBLIC API — no authentication required.**

            Returns a ranked list of search suggestions from three sources:
            - **TITLE** — listing titles that begin with the normalized query.
            - **LOCATION** — province / district / ward names that match the query.
            - **POPULAR_QUERY** — historically popular search terms derived from click telemetry.

            ### Normalization
            The query is normalized with `TextNormalizer` (Vietnamese-safe: strips diacritics,
            lowercases, collapses whitespace). Queries that are shorter than **2 characters**
            after normalization return an empty suggestion list immediately.

            ### Caching
            Results are short-cached (2 minutes by default). The cache key includes the
            normalized query, `provinceId`, `categoryId`, and `limit`.

            ### Telemetry
            Each call records an impression in `search_query_impressions`.
            The `impressionId` field in the response should be passed back to
            `POST /v1/listings/search-suggestions/click` when the user selects a suggestion.
            """,
        parameters = {
            @Parameter(name = "q",          description = "Raw search query",                             required = true,  example = "căn hộ quận 1"),
            @Parameter(name = "limit",      description = "Max suggestions to return (1-20)",             required = false, example = "8"),
            @Parameter(name = "provinceId", description = "Legacy province ID for scoped filtering",      required = false, example = "79"),
            @Parameter(name = "categoryId", description = "Category ID for scoped filtering",             required = false, example = "1"),
            @Parameter(name = "X-Session-Id", description = "Optional opaque session token (header) for telemetry correlation", required = false, example = "sess_abc123")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Suggestion list (may be empty if query is too short or no candidates found)",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Sample response",
                        value = """
                            {
                              "code": 1000,
                              "data": {
                                "suggestions": [
                                  { "type": "TITLE",    "text": "Căn hộ 2PN Quận 1 full nội thất", "listingId": 12345, "score": 1.5,  "metadata": { "address": "123 Nguyễn Trãi, Quận 1, TP.HCM" } },
                                  { "type": "LOCATION", "text": "Quận 1, TP. Hồ Chí Minh",         "listingId": null,  "score": 1.0,  "metadata": { "provinceName": "TP. Hồ Chí Minh", "districtName": "Quận 1" } },
                                  { "type": "POPULAR_QUERY", "text": "căn hộ 2 phòng ngủ",         "listingId": null,  "score": 0.8,  "metadata": { "hitCount": 42 } }
                                ],
                                "queryNorm": "can ho quan 1",
                                "impressionId": 9876
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    public com.smartrent.dto.response.ApiResponse<SearchSuggestionsResponse> getSearchSuggestions(
            @RequestParam(value = "q", required = false, defaultValue = "") String query,
            @RequestParam(value = "limit",      defaultValue = "8")  int    limit,
            @RequestParam(value = "provinceId", required = false)     String provinceId,
            @RequestParam(value = "categoryId", required = false)     Long   categoryId,
            HttpServletRequest httpRequest) {

        String clientIp   = extractClientIp(httpRequest);
        String sessionId  = httpRequest.getHeader("X-Session-Id");

        SearchSuggestionsResponse response = searchSuggestionService.getSuggestions(
                query, limit, provinceId, categoryId, clientIp, sessionId);

        return com.smartrent.dto.response.ApiResponse
                .<SearchSuggestionsResponse>builder()
                .data(response)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Extracts the best-effort client IP from the incoming request.
     * Checks {@code X-Forwarded-For} first (proxy / load-balancer scenario),
     * then falls back to {@code RemoteAddr}.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; first entry is the client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/map-bounds")
    @Operation(
        summary = "[PUBLIC API] Get listings within map bounds for interactive map display",
        description = """
            **PUBLIC API - Không cần authentication**

            API để lấy danh sách bài đăng trong vùng hiển thị trên bản đồ (map bounds).
            Được thiết kế cho tính năng bản đồ tương tác (interactive map).
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MapBoundsRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Basic Map Bounds",
                        summary = "Vùng bản đồ cơ bản - TP.HCM trung tâm",
                        value = """
                            {
                              "neLat": 10.823,
                              "neLng": 106.701,
                              "swLat": 10.705,
                              "swLng": 106.590,
                              "zoom": 14,
                              "limit": 100
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved listings within map bounds",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
                )
            )
        }
    )
    public ApiResponse<MapListingsResponse> getListingsByMapBounds(
            @Valid @RequestBody MapBoundsRequest request) {
        MapListingsResponse response = listingService.getListingsByMapBounds(request);
        return ApiResponse.<MapListingsResponse>builder().data(response).build();
    }
}
