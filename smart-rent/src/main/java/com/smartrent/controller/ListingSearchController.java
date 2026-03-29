package com.smartrent.controller;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.MapBoundsRequest;
import com.smartrent.dto.response.*;
import com.smartrent.service.listing.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ApiResponse<ListingListResponse> searchListings(
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

        ListingListResponse response = listingService.searchListings(filter);
        return ApiResponse.<ListingListResponse>builder().data(response).build();
    }

    @GetMapping("/sellers/{userId}/diamond")
    @Operation(
        summary = "[PUBLIC API] Seller DIAMOND listings",
        description = "Get paginated DIAMOND listings for a public seller profile section."
    )
    public ApiResponse<ListingListResponse> getSellerDiamondListings(
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
    public ApiResponse<ListingListResponse> getSellerGoldListings(
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
    public ApiResponse<ListingListResponse> getSellerSilverListings(
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
    public ApiResponse<ListingListResponse> getSellerNormalListings(
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
    public ApiResponse<ListingListResponse> getSellerTopSavedListings(
            @PathVariable String userId,
            @RequestParam(defaultValue = "5") Integer limit) {
        int safeLimit = limit != null && limit > 0 ? Math.min(limit, 20) : 5;
        ListingListResponse response = listingService.getTopSavedListingsByUser(userId, safeLimit);
        return ApiResponse.<ListingListResponse>builder().data(response).build();
    }

    private ApiResponse<ListingListResponse> searchSellerListingsByVipType(
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

        ListingListResponse response = listingService.searchListings(filter);
        return ApiResponse.<ListingListResponse>builder().data(response).build();
    }

    @GetMapping("/autocomplete")
    @Operation(
        summary = "Autocomplete listing titles",
        description = "Returns lightweight listing suggestions by title prefix."
    )
    public ApiResponse<List<ListingAutocompleteResponse>> autocompleteListings(
            @Parameter(description = "Query prefix", required = true, example = "can ho")
            @RequestParam("q") String query,
            @Parameter(description = "Max results (1-20)", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<ListingAutocompleteResponse> results = listingService.autocompleteListings(query, limit);
        return ApiResponse.<List<ListingAutocompleteResponse>>builder().data(results).build();
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
