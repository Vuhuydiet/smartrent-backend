package com.smartrent.controller;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingDraftUpdateRequest;
import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.MyListingsFilterRequest;
import com.smartrent.dto.request.ProvinceStatsRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.BenefitType;
import com.smartrent.dto.response.ListingListResponse;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.quota.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Property Listings",
    description = """
        Complete property listing management with VIP tiers, quota system, and transactional address creation.

        **Listing Types:**
        - NORMAL: Standard listings (90,000 VND/30 days)
        - SILVER: Enhanced visibility (600,000 VND/30 days or quota)
        - GOLD: Premium visibility (1,200,000 VND/30 days or quota)
        - DIAMOND: Maximum visibility + shadow NORMAL listing (1,800,000 VND/30 days or quota)

        **Payment Model:**
        - Pay-per-post: Direct payment via VNPay
        - Membership quota: Free posts from membership package

        **Features:**
        - Transactional address creation (no orphaned data)
        - Location-based pricing analytics
        - Admin verification workflow
        - Quota management
        """
)
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final QuotaService quotaService;

    @Operation(
        summary = "Create a new listing with transactional address",
        description = """
            Creates a new property listing with automatic address creation in a single transaction.

            **Two Creation Modes:**

            1. **Standard Listing (isDraft=false or not provided):**
               - All required fields must be provided
               - Listing is published immediately (subject to verification)
               - Payment flow initiated if durationDays is provided

            2. **Draft Listing (isDraft=true):**
               - User ID is automatically extracted from authentication token
               - All other fields are optional
               - Listing saved as draft (not visible in public search)
               - Useful for saving incomplete listings when user loses connection
               - Can be completed and published later via update API

            **Payment Flow:**
            1. Call GET /v1/vip-tiers/{tierCode} to get VIP tier information including available durations and prices
            2. User selects vipType (NORMAL, SILVER, GOLD, DIAMOND) and durationDays from available options (10, 15, 30)
            3. Submit this request with durationDays - system calculates price based on VIP tier and duration
            4. Complete payment via returned paymentUrl
            5. Listing is created after successful payment

            **Duration Options (with discounts):**
            - 10 days (no discount)
            - 15 days (with discount based on VIP tier)
            - 30 days (with discount based on VIP tier)

            Each VIP tier has its own pricing: Check GET /v1/vip-tiers/{tierCode} for exact prices (price10Days, price15Days, price30Days)

            **Transactional Address Creation:**
            - Address is created first within the same transaction
            - If listing creation fails, address is automatically rolled back
            - No orphaned address data
            - Full address text is auto-generated if not provided

            **Address Structure (Two Formats Supported):**

            **Nested Format:**
            ```json
            "address": {
              "legacy": {
                "provinceId": 1,
                "districtId": 5,
                "wardId": 20,
                "street": "123 Nguyen Trai"
              },
              "newAddress": {
                "provinceCode": "01",
                "wardCode": "00004",
                "street": "88 Le Duan"
              },
              "latitude": 21.0285,
              "longitude": 105.8542
            }
            ```
         
            **Required Fields for Standard Listing:**
            - title, description
            - listingType (RENT/SALE/SHARE)
            - vipType (NORMAL/SILVER/GOLD/DIAMOND)
            - categoryId
            - productType (APARTMENT/HOUSE/STUDIO/ROOM/OFFICE)
            - price, priceUnit (MONTH/DAY/YEAR)
            - address object with:
              - **Nested format:**
                - For legacy: legacy.provinceId, legacy.districtId, legacy.wardId
                - For new: newAddress.provinceCode, newAddress.wardCode
              - **Flat format (deprecated):**
                - addressType (OLD or NEW)
                - For OLD: provinceId, districtId, wardId
                - For NEW: newProvinceCode, newWardCode

            **Optional Fields:**
            - isDraft (default: false)
            - area, bedrooms, bathrooms
            - direction, furnishing
            - roomCapacity
            - amenityIds (array of amenity IDs)
            - mediaIds (array of uploaded media IDs) - See Media Upload Integration Flow above
            - address.legacy.street or address.newAddress.street (street name as string)
            - address.streetId (street reference - flat format)
            - address.projectId (building/complex reference)
            - address.latitude, address.longitude
            - durationDays (Integer: 10, 15, or 30 for payment flow)
            - useMembershipQuota (for VIP listings with existing quota)
            - paymentProvider (VNPAY, etc.)
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingCreationRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Listing Creation",
                        summary = "Create listing fully with all details",
                        value = """
                            {
                              "title": "Căn hộ 2 phòng ngủ ấm cúng trung tâm thành phố",
                              "description": "Căn hộ rộng rãi 2 phòng ngủ có ban công và tầm nhìn thành phố.",
                              "listingType": "RENT",
                              "vipType": "NORMAL",
                              "categoryId": 1,
                              "productType": "APARTMENT",
                              "price": 12000000,
                              "priceUnit": "MONTH",
                              "address": {
                                "legacy": {
                                  "provinceId": 1,
                                  "districtId": 5,
                                  "wardId": 20,
                                  "street": "123 Nguyễn Trãi"
                                },
                                "newAddress": {
                                  "provinceCode": "01",
                                  "wardCode": "00004",
                                  "street": "88 Lê Duẩn"
                                },
                                "latitude": 21.0285,
                                "longitude": 105.8542
                              },
                              "area": 78.5,
                              "bedrooms": 2,
                              "bathrooms": 1,
                              "direction": "NORTHEAST",
                              "furnishing": "SEMI_FURNISHED",
                              "roomCapacity": 4,
                              "amenityIds": [1, 3, 5],
                              "mediaIds": [101, 102, 103],
                              "isDraft": false,
                              "durationDays": 30,
                              "waterPrice": "NEGOTIABLE",
                              "electricityPrice": "SET_BY_OWNER",
                              "internetPrice": "PROVIDER_RATE",
                              "useMembershipQuota": false,
                              "benefitsMembership": ["SILVER"]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Draft Listing",
                        summary = "Save incomplete listing as draft (for auto-save/connection loss)",
                        value = """
                            {
                              "title": "Căn hộ 2 phòng ngủ",
                              "description": "Căn hộ đang cập nhật thông tin...",
                              "listingType": "RENT",
                              "categoryId": 1,
                              "price": 12000000,
                              "isDraft": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Listing with Payment (NORMAL tier)",
                        summary = "Create NORMAL listing with duration (requires payment) - Using nested new address",
                        value = """
                            {
                              "title": "Phòng trọ sinh viên giá rẻ",
                              "description": "Phòng trọ tiện nghi, gần trường đại học",
                              "listingType": "RENT",
                              "vipType": "NORMAL",
                              "categoryId": 1,
                              "productType": "ROOM",
                              "price": 3000000,
                              "priceUnit": "MONTH",
                              "address": {
                                "newAddress": {
                                  "provinceCode": "01",
                                  "wardCode": "00001",
                                  "street": "45 Đại Cồ Việt"
                                },
                                "latitude": 21.0285,
                                "longitude": 105.8542
                              },
                              "area": 25.0,
                              "bedrooms": 1,
                              "bathrooms": 1,
                              "roomCapacity": 2,
                              "durationDays": 30,
                              "paymentProvider": "VNPAY"
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing created or payment required",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                        @ExampleObject(
                            name = "Standard/Draft Listing Created",
                            summary = "Listing created successfully",
                            value = """
                                {
                                  "code": "999999",
                                  "message": null,
                                  "data": {
                                    "listingId": 123,
                                    "status": "CREATED"
                                  }
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Payment Required (NORMAL tier)",
                            summary = "Payment URL returned for listing with duration",
                            value = """
                                {
                                  "code": "999999",
                                  "message": null,
                                  "data": {
                                    "paymentRequired": true,
                                    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
                                    "amount": 90000,
                                    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=9000000&vnp_Command=pay...",
                                    "message": "Payment required. Complete payment to create listing."
                                  }
                                }
                                """
                        )
                    }
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation error - missing required fields for non-draft listing",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Missing Fields",
                        value = """
                            {
                              "code": "400001",
                              "message": "Missing required fields for non-draft listing: title, description, address",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    @PostMapping
    public ApiResponse<ListingCreationResponse> createListing(@Valid @RequestBody ListingCreationRequest request) {
        String userId = extractUserId();
        request.setUserId(userId);
        ListingCreationResponse response = listingService.createListing(request);
        return ApiResponse.<ListingCreationResponse>builder().data(response).build();
    }

    @Operation(
        summary = "Get listing by ID",
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Listing Example",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "listingId": 123,
                                "title": "Căn hộ 2 phòng ngủ ấm cúng trung tâm thành phố",
                                "description": "Căn hộ rộng rãi 2 phòng ngủ có ban công và tầm nhìn thành phố.",
                                "user": {
                                  "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                  "phoneCode": "+84",
                                  "phoneNumber": "912345678",
                                  "email": "owner@example.com",
                                  "firstName": "Nguyen",
                                  "lastName": "Van A",
                                  "contactPhoneNumber": "0912345678",
                                  "contactPhoneVerified": true
                                },
                                "postDate": "2025-09-01T10:00:00",
                                "expiryDate": "2025-12-31T23:59:59",
                                "listingType": "RENT",
                                "verified": false,
                                "isVerify": false,
                                "expired": false,
                                "vipType": "NORMAL",
                                "categoryId": 10,
                                "productType": "APARTMENT",
                                "price": 1200.00,
                                "priceUnit": "MONTH",
                                "address": {
                                  "addressId": 501,
                                  "fullAddress": "123 Nguyễn Trãi, Phường 5, Quận 5, Thành Phố Hồ Chí Minh",
                                  "fullNewAddress": "123 Nguyễn Trãi, Phường Bến Nghé, Thành Phố Hồ Chí Minh",
                                  "latitude": 10.7545,
                                  "longitude": 106.6679,
                                  "addressType": "OLD",
                                  "legacyProvinceId": 79,
                                  "legacyProvinceName": "Thành Phố Hồ Chí Minh",
                                  "legacyDistrictId": 765,
                                  "legacyDistrictName": "Quận 5",
                                  "legacyWardId": 26800,
                                  "legacyWardName": "Phường 5",
                                  "legacyStreet": "Nguyễn Trãi",
                                  "newProvinceCode": "79",
                                  "newProvinceName": "Thành Phố Hồ Chí Minh",
                                  "newWardCode": "26734",
                                  "newWardName": "Phường Bến Nghé",
                                  "newStreet": "Nguyễn Trãi"
                                },
                                "area": 78.5,
                                "bedrooms": 2,
                                "bathrooms": 1,
                                "direction": "NORTHEAST",
                                "furnishing": "SEMI_FURNISHED",
                                "roomCapacity": 4,
                                "amenities": [
                                  {
                                    "amenityId": 1,
                                    "name": "Điều hòa",
                                    "icon": "ac-icon",
                                    "description": "Máy điều hòa không khí",
                                    "category": "BASIC",
                                    "isActive": true
                                  },
                                  {
                                    "amenityId": 3,
                                    "name": "Máy giặt",
                                    "icon": "washing-machine-icon",
                                    "description": "Máy giặt tự động",
                                    "category": "BASIC",
                                    "isActive": true
                                  },
                                  {
                                    "amenityId": 5,
                                    "name": "WiFi miễn phí",
                                    "icon": "wifi-icon",
                                    "description": "Kết nối internet tốc độ cao",
                                    "category": "CONVENIENCE",
                                    "isActive": true
                                  }
                                ],
                                "media": [
                                  {
                                    "mediaId": 1,
                                    "listingId": 123,
                                    "mediaType": "IMAGE",
                                    "url": "https://placehold.co/400x300/cccccc/969696.png?font=lato",
                                    "isPrimary": true,
                                    "sortOrder": 0,
                                    "status": "ACTIVE",
                                    "createdAt": "2025-09-01T10:00:00"
                                  },
                                  {
                                    "mediaId": 2,
                                    "listingId": 123,
                                    "mediaType": "IMAGE",
                                    "url": "https://placehold.co/400x300/cccccc/969696.png?font=lato",
                                    "isPrimary": false,
                                    "sortOrder": 1,
                                    "status": "ACTIVE",
                                    "createdAt": "2025-09-01T10:05:00"
                                  },
                                  {
                                    "mediaId": 3,
                                    "listingId": 123,
                                    "mediaType": "IMAGE",
                                    "url": "https://placehold.co/400x300/cccccc/969696.png?font=lato",
                                    "isPrimary": false,
                                    "sortOrder": 2,
                                    "status": "ACTIVE",
                                    "createdAt": "2025-09-01T10:10:00"
                                  }
                                ],
                                "locationPricing": {
                                  "wardPricing": {
                                    "locationType": "WARD",
                                    "locationId": 1001,
                                    "locationName": "Phường Bến Nghé",
                                    "totalListings": 45,
                                    "averagePrice": 1500000,
                                    "minPrice": 800000,
                                    "maxPrice": 3500000,
                                    "medianPrice": 1400000,
                                    "priceUnit": "MONTH",
                                    "productType": "APARTMENT",
                                    "averageArea": 65.5,
                                    "averagePricePerSqm": 22900
                                  },
                                  "districtPricing": {
                                    "locationType": "DISTRICT",
                                    "locationId": 100,
                                    "totalListings": 320,
                                    "averagePrice": 1600000,
                                    "priceUnit": "MONTH"
                                  },
                                  "provincePricing": {
                                    "locationType": "PROVINCE",
                                    "locationId": 10,
                                    "totalListings": 5420,
                                    "averagePrice": 1750000,
                                    "priceUnit": "MONTH"
                                  },
                                  "similarListingsInWard": [
                                    {
                                      "listingId": 124,
                                      "title": "Căn hộ 2PN tương tự",
                                      "price": 1100000,
                                      "priceUnit": "MONTH",
                                      "area": 70.0,
                                      "pricePerSqm": 15714,
                                      "bedrooms": 2,
                                      "bathrooms": 1,
                                      "productType": "APARTMENT",
                                      "vipType": "NORMAL",
                                      "verified": true
                                    }
                                  ],
                                  "priceComparison": "BELOW_AVERAGE",
                                  "percentageDifferenceFromAverage": -20.0
                                },
                                "createdAt": "2025-09-01T10:00:00",
                                "updatedAt": "2025-09-01T10:00:00"
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found",
                        value = """
                            {
                              "code": "404001",
                              "message": "LISTING_NOT_FOUND",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    @GetMapping("/{id}")
    public ApiResponse<ListingResponse> getListingById(@PathVariable Long id) {
        ListingResponse response = listingService.getListingById(id);
        return ApiResponse.<ListingResponse>builder().data(response).build();
    }

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
              - EXPIRED: Hết hạn
              - EXPIRING_SOON: Sắp hết hạn (còn 7 ngày)
              - DISPLAYING: Đang hiển thị
              - IN_REVIEW: Đang chờ duyệt
              - PENDING_PAYMENT: Chờ thanh toán
              - REJECTED: Bị từ chối
              - VERIFIED: Đã xác thực

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
            - `sortBy`: postDate, price, area, distance, createdAt, updatedAt
            - `sortDirection`: ASC, DESC (mặc định)

            ## RESPONSE
            - Danh sách bài đăng có phân trang
            - Tổng số bài (`totalCount`), thông tin phân trang
            - Danh sách gợi ý (top 5 GOLD/DIAMOND cùng danh mục)
            - Filter criteria đã áp dụng

            ## LƯU Ý
            - **Public API**: Không yêu cầu JWT token
            - **Mặc định**: Chỉ trả về bài đăng công khai (excludes drafts, shadow listings, expired listings)
            - **Tất cả filter đều optional**: Frontend chỉ gửi các field cần thiết
            - **Có thể kết hợp nhiều filter**: Hỗ trợ filter phức tạp
            - **Auto-filter cho public search**: Bài nháp (isDraft), bài shadow, bài hết hạn tự động bị loại trừ

            ## USE CASES

            **Use Case 1: Màn hình chính - Tất cả bài đăng**
            ```json
            {"verified": true, "excludeExpired": true, "page": 0, "size": 20, "sortBy": "postDate", "sortDirection": "DESC"}
            ```

            **Use Case 2: Lọc căn hộ cho thuê tại Hà Nội**
            ```json
            {"provinceId": "1", "isLegacy": true, "listingType": "RENT", "productType": "APARTMENT", "verified": true, "hasMedia": true, "page": 0, "size": 20}
            ```

            **Use Case 3: Tìm nhà giá 5-15 triệu/tháng, 2-3 phòng ngủ**
            ```json
            {"provinceId": "1", "isLegacy": true, "minPrice": 5000000, "maxPrice": 15000000, "priceUnit": "MONTH", "minBedrooms": 2, "maxBedrooms": 3, "verified": true, "sortBy": "price"}
            ```

            **Use Case 4: Bài đăng đang giảm giá**
            ```json
            {"hasPriceReduction": true, "minPriceReductionPercent": 10, "priceChangedWithinDays": 30, "verified": true, "hasMedia": true}
            ```

            **Use Case 5: Tìm nhà gần vị trí hiện tại (GPS)**
            ```json
            {"latitude": 21.0285, "longitude": 105.8542, "radiusKm": 5.0, "verified": true, "sortBy": "distance"}
            ```

            **Use Case 6: Lọc theo tiện ích (điều hòa + WiFi + máy giặt)**
            ```json
            {"amenityIds": [1, 3, 5], "amenityMatchMode": "ALL", "provinceId": "1", "isLegacy": true, "verified": true}
            ```

            **Use Case 7: Tìm kiếm theo từ khóa**
            ```json
            {"keyword": "căn hộ cao cấp view biển", "provinceId": "48", "isLegacy": true, "verified": true, "hasMedia": true}
            ```

            **Use Case 8: Tin mới nhất (trong 7 ngày)**
            ```json
            {"postedWithinDays": 7, "verified": true, "hasMedia": true, "sortBy": "postDate", "sortDirection": "DESC"}
            ```

            **Use Case 9: Chỉ lấy bài VIP (GOLD/DIAMOND)**
            ```json
            {"vipType": "GOLD", "verified": true, "sortBy": "postDate", "sortDirection": "DESC"}
            ```

            **Use Case 10: Lọc đầy đủ - Căn hộ cao cấp**
            ```json
            {"provinceId": "1", "isLegacy": true, "listingType": "RENT", "productType": "APARTMENT", "minBedrooms": 2, "maxBedrooms": 3, "minPrice": 10000000, "maxPrice": 20000000, "priceUnit": "MONTH", "minArea": 60.0, "furnishing": "FULLY_FURNISHED", "direction": "SOUTH", "waterPrice": "LOW", "electricityPrice": "LOW", "internetPrice": "FREE", "amenityIds": [1, 3, 5, 7], "amenityMatchMode": "ALL", "hasMedia": true, "verified": true, "ownerPhoneVerified": true}
            ```

            **Use Case 11: Tìm phòng trọ giá rẻ - Theo cấu trúc mới (34 tỉnh)**
            ```json
            {"provinceCode": "79", "isLegacy": false, "listingType": "RENT", "productType": "ROOM", "maxPrice": 3000000, "priceUnit": "MONTH", "waterPrice": "LOW", "electricityPrice": "LOW", "internetPrice": "FREE", "verified": true, "status": "ACTIVE"}
            ```

            **Use Case 12: Owner - Listings đang hiển thị**
            ```json
            {"userId": "user-123", "listingStatus": "DISPLAYING", "page": 0, "size": 20}
            ```

            **Use Case 13: Owner - Listings sắp hết hạn**
            ```json
            {"userId": "user-123", "listingStatus": "EXPIRING_SOON", "sortBy": "expiryDate", "sortDirection": "ASC"}
            ```

            **Use Case 14: Owner - Listings đang chờ duyệt**
            ```json
            {"userId": "user-123", "listingStatus": "IN_REVIEW", "SortBy": "DEFAULT", "sortDirection": "DESC"}
            ```

            **Use Case 15: Owner - Listings bị từ chối**
            ```json
            {"userId": "user-123", "listingStatus": "REJECTED"}
            ```
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingFilterRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Request Example - Đầy đủ tất cả filter",
                        summary = "Tham khảo tất cả các filter có thể sử dụng (Frontend chỉ cần gửi các field cần thiết)",
                        value = """
                            {
                              "userId": "user-123",
                              "isDraft": false,
                              "verified": true,
                              "isVerify": false,
                              "expired": false,
                              "excludeExpired": true,
                              "status": "ACTIVE",
                              "provinceId": "1",
                              "provinceCode": "01",
                              "districtId": 5,
                              "wardId": "123",
                              "newWardCode": "00001",
                              "streetId": 10,
                              "isLegacy": true,
                              "latitude": 21.0285,
                              "longitude": 105.8542,
                              "userLatitude": 21.0285,
                              "userLongitude": 105.8542,
                              "radiusKm": 5.0,
                              "categoryId": 1,
                              "listingType": "RENT",
                              "vipType": "GOLD",
                              "productType": "APARTMENT",
                              "minPrice": 5000000,
                              "maxPrice": 15000000,
                              "priceUnit": "MONTH",
                              "hasPriceReduction": true,
                              "hasPriceIncrease": false,
                              "minPriceReductionPercent": 10,
                              "maxPriceReductionPercent": 50,
                              "priceChangedWithinDays": 30,
                              "minArea": 50.0,
                              "maxArea": 100.0,
                              "bedrooms": null,
                              "bathrooms": null,
                              "minBedrooms": 2,
                              "maxBedrooms": 3,
                              "minBathrooms": 1,
                              "maxBathrooms": 2,
                              "furnishing": "FULLY_FURNISHED",
                              "direction": "SOUTH",
                              "minRoomCapacity": 2,
                              "maxRoomCapacity": 4,
                              "waterPrice": "LOW",
                              "electricityPrice": "MEDIUM",
                              "internetPrice": "FREE",
                              "serviceFee": "LOW",
                              "amenityIds": [1, 3, 5],
                              "amenityMatchMode": "ALL",
                              "hasMedia": true,
                              "minMediaCount": 3,
                              "keyword": "căn hộ cao cấp view đẹp",
                              "ownerPhoneVerified": true,
                              "postedWithinDays": 7,
                              "updatedWithinDays": 3,
                              "page": 0,
                              "size": 20,
                              "sortBy": "postDate",
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
                description = "Search results with pagination and recommendations",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Unified Search Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "listings": [
                                  {
                                    "listingId": 123,
                                    "title": "Căn hộ 2PN cao cấp view Hồ Tây",
                                    "description": "Căn hộ đầy đủ nội thất, view đẹp",
                                    "user": {
                                      "userId": "user-abc-123",
                                      "firstName": "Nguyen",
                                      "lastName": "Van B",
                                      "email": "owner@example.com",
                                      "contactPhoneNumber": "0987654321",
                                      "contactPhoneVerified": true
                                    },
                                    "price": 12000000,
                                    "priceUnit": "MONTH",
                                    "address": {
                                      "addressId": 601,
                                      "fullAddress": "50 Yên Phụ, Phường Yên Phụ, Quận Tây Hồ, Thành Phố Hà Nội",
                                      "fullNewAddress": "50 Yên Phụ, Phường Yên Phụ, Thành Phố Hà Nội",
                                      "latitude": 21.0562,
                                      "longitude": 105.8251,
                                      "addressType": "OLD",
                                      "legacyProvinceId": 1,
                                      "legacyProvinceName": "Thành Phố Hà Nội",
                                      "legacyDistrictId": 17,
                                      "legacyDistrictName": "Quận Tây Hồ",
                                      "legacyWardId": 608,
                                      "legacyWardName": "Phường Yên Phụ",
                                      "legacyStreet": "Yên Phụ",
                                      "newProvinceCode": "01",
                                      "newProvinceName": "Thành Phố Hà Nội",
                                      "newWardCode": "00268",
                                      "newWardName": "Phường Yên Phụ",
                                      "newStreet": "Yên Phụ"
                                    },
                                    "area": 78.5,
                                    "bedrooms": 2,
                                    "bathrooms": 1,
                                    "vipType": "SILVER",
                                    "listingType": "RENT",
                                    "productType": "APARTMENT",
                                    "verified": true,
                                    "isVerify": false,
                                    "expired": false,
                                    "isDraft": false,
                                    "postDate": "2025-11-15T10:00:00"
                                  }
                                ],
                                "totalCount": 150,
                                "currentPage": 0,
                                "pageSize": 20,
                                "totalPages": 8,
                                "recommendations": [
                                  {
                                    "listingId": 456,
                                    "title": "Penthouse DIAMOND cao cấp",
                                    "vipType": "DIAMOND",
                                    "verified": true
                                  }
                                ],
                                "filterCriteria": {
                                  "categoryId": 1,
                                  "provinceId": 1,
                                  "districtId": 5,
                                  "listingType": "RENT",
                                  "verified": true
                                }
                              }
                            }
                            """
                    )
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

        // PUBLIC API: This endpoint supports both authenticated and non-authenticated requests
        // - Public users (authentication == null): Can search but won't see drafts/private data
        // - Authenticated users: Can search AND request their own listings by omitting userId in filter

        // Auto-fill userId from JWT for authenticated users ONLY when requesting ownership-specific data
        // This allows frontend to omit userId for "my listings" - backend auto-fills from JWT
        if ((filter.getUserId() == null || filter.getUserId().isEmpty()) &&
            authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getName())) {

            // Check if this is a "my listings" request by looking at isDraft or other ownership filters
            if (filter.getIsDraft() != null || filter.getIsVerify() != null) {
                filter.setUserId(authentication.getName());
            }
        }

        // Service layer will automatically filter out drafts, shadow listings, and expired listings
        // when userId is not provided (public search)
        ListingListResponse response = listingService.searchListings(filter);
        return ApiResponse.<ListingListResponse>builder().data(response).build();
    }

    @Deprecated
    @PostMapping("/my-listings")
    @Operation(
        summary = "[DEPRECATED] Get current user's listings - Use /search instead",
        description = """
            **DEPRECATED - Use POST /v1/listings/search instead**

            This endpoint is deprecated. Please migrate to the unified search API:

            ```
            POST /v1/listings/search
            {
              "userId": "your-user-id",  // Or omit - auto-filled from JWT if isDraft/isVerify present
              "isDraft": true,           // Filter for drafts
              "verified": true,          // Filter for verified
              "page": 0,
              "size": 20
            }
            ```

            This endpoint still works but redirects internally to the unified search API.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MyListingsFilterRequest.class),
                examples = {
                    @ExampleObject(
                        name = "My Draft Listings",
                        summary = "Get all draft listings (incomplete/auto-saved)",
                        value = """
                            {
                              "isDraft": true,
                              "page": 0,
                              "size": 20,
                              "
                              ": "DEFAULT",
                              "sortDirection": "DESC"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "My Active Listings",
                        summary = "Get published, verified, non-expired listings",
                        value = """
                            {
                              "verified": true,
                              "expired": false,
                              "isDraft": false,
                              "page": 0,
                              "size": 20,
                              "sortBy": "DEFAULT",
                              "sortDirection": "DESC"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Pending Verification",
                        summary = "Get listings awaiting admin verification",
                        value = """
                            {
                              "isVerify": true,
                              "verified": false,
                              "page": 0,
                              "size": 20,
                              "SortBy": "DEFAULT",
                              "sortDirection": "ASC"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Expired Listings",
                        summary = "Get expired listings needing renewal",
                        value = """
                            {
                              "expired": true,
                              "page": 0,
                              "size": 20,
                              "SortBy": "DEFAULT",
                              "sortDirection": "DESC"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Premium Listings Only",
                        summary = "Get my GOLD and DIAMOND tier listings",
                        value = """
                            {
                              "vipType": "GOLD",
                              "expired": false,
                              "page": 0,
                              "size": 20
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "All My Listings",
                        summary = "Get all my listings without filters",
                        value = """
                            {
                              "page": 0,
                              "size": 20,
                              "SortBy": "DEFAULT",
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
                description = "User's listings with pagination",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "My Listings Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "listings": [
                                  {
                                    "listingId": 123,
                                    "title": "Căn hộ 2PN của tôi",
                                    "userId": "user-123",
                                    "price": 12000000,
                                    "priceUnit": "MONTH",
                                    "verified": true,
                                    "isVerify": false,
                                    "expired": false,
                                    "isDraft": false,
                                    "vipType": "SILVER",
                                    "listingType": "RENT"
                                  },
                                  {
                                    "listingId": 124,
                                    "title": "Draft - Nhà mới chưa hoàn thành",
                                    "userId": "user-123",
                                    "verified": false,
                                    "isVerify": false,
                                    "expired": false,
                                    "isDraft": true,
                                    "vipType": "NORMAL"
                                  }
                                ],
                                "totalCount": 8,
                                "currentPage": 0,
                                "pageSize": 20,
                                "totalPages": 1,
                                "recommendations": [],
                                "filterCriteria": {
                                  "page": 0,
                                  "size": 20
                                }
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<ListingListResponse> getMyListings(
            @RequestBody(required = false) MyListingsFilterRequest filter,
            Authentication authentication) {

        // Redirect to unified search API
        String userId = authentication != null ? authentication.getName() : null;

        if (filter == null) {
            filter = MyListingsFilterRequest.builder().build();
        }

        // Convert MyListingsFilterRequest to unified ListingFilterRequest
        ListingFilterRequest unifiedFilter = ListingFilterRequest.builder()
                .userId(userId)
                .verified(filter.getVerified())
                .isVerify(filter.getIsVerify())
                .expired(filter.getExpired())
                .isDraft(filter.getIsDraft())
                .vipType(filter.getVipType())
                .listingType(filter.getListingType())
                .page(filter.getPage())
                .size(filter.getSize())
                .sortBy(filter.getSortBy())
                .sortDirection(filter.getSortDirection())
                .excludeExpired(false) // Don't auto-exclude for my listings
                .build();

        ListingListResponse response = listingService.searchListings(unifiedFilter);
        return ApiResponse.<ListingListResponse>builder().data(response).build();
    }

    @GetMapping
    @Operation(
        summary = "List listings",
        description = "When 'ids' is provided, returns those listings. Otherwise returns a paginated list using 'page' and 'size'.",
        parameters = {
            @Parameter(name = "ids", description = "Optional list of listing IDs to fetch explicitly"),
            @Parameter(name = "page", description = "Zero-based page index", example = "0"),
            @Parameter(name = "size", description = "Page size (max 100)", example = "20")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "List of listings",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Listings Example",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": [
                                {
                                  "listingId": 123,
                                  "title": "Căn hộ 2 phòng ngủ ấm cúng trung tâm thành phố",
                                  "description": "Căn hộ rộng rãi 2 phòng ngủ có ban công và tầm nhìn thành phố.",
                                  "user": {
                                    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                    "firstName": "Tran",
                                    "lastName": "Van C",
                                    "email": "user1@example.com",
                                    "contactPhoneNumber": "0901234567",
                                    "contactPhoneVerified": true
                                  },
                                  "postDate": "2025-09-01T10:00:00",
                                  "expiryDate": "2025-12-31T23:59:59",
                                  "listingType": "RENT",
                                  "verified": false,
                                  "isVerify": false,
                                  "expired": false,
                                  "vipType": "NORMAL",
                                  "categoryId": 10,
                                  "productType": "APARTMENT",
                                  "price": 1200.00,
                                  "priceUnit": "MONTH",
                                  "address": {
                                    "addressId": 501,
                                    "fullAddress": "123 Nguyễn Trãi, Phường 5, Quận 5, Thành Phố Hồ Chí Minh",
                                    "fullNewAddress": "123 Nguyễn Trãi, Phường Bến Nghé, Thành Phố Hồ Chí Minh",
                                    "latitude": 10.7545,
                                    "longitude": 106.6679,
                                    "addressType": "OLD",
                                    "legacyProvinceId": 79,
                                    "legacyProvinceName": "Thành Phố Hồ Chí Minh",
                                    "legacyDistrictId": 765,
                                    "legacyDistrictName": "Quận 5",
                                    "legacyWardId": 26800,
                                    "legacyWardName": "Phường 5",
                                    "legacyStreet": "Nguyễn Trãi",
                                    "newProvinceCode": "79",
                                    "newProvinceName": "Thành Phố Hồ Chí Minh",
                                    "newWardCode": "26734",
                                    "newWardName": "Phường Bến Nghé",
                                    "newStreet": "Nguyễn Trãi"
                                  },
                                  "area": 78.5,
                                  "bedrooms": 2,
                                  "bathrooms": 1,
                                  "direction": "NORTHEAST",
                                  "furnishing": "SEMI_FURNISHED",
                                  "propertyType": "APARTMENT",
                                  "roomCapacity": 4,
                                  "amenities": [
                                    {
                                      "amenityId": 1,
                                      "name": "Điều hòa",
                                      "icon": "ac-icon",
                                      "description": "Máy điều hòa không khí",
                                      "category": "BASIC",
                                      "isActive": true
                                    },
                                    {
                                      "amenityId": 3,
                                      "name": "Máy giặt",
                                      "icon": "washing-machine-icon",
                                      "description": "Máy giặt tự động",
                                      "category": "BASIC",
                                      "isActive": true
                                    },
                                    {
                                      "amenityId": 5,
                                      "name": "WiFi miễn phí",
                                      "icon": "wifi-icon",
                                      "description": "Kết nối internet tốc độ cao",
                                      "category": "CONVENIENCE",
                                      "isActive": true
                                    }
                                  ],
                                  "createdAt": "2025-09-01T10:00:00",
                                  "updatedAt": "2025-09-01T10:00:00"
                                },
                                {
                                  "listingId": 124,
                                  "title": "Căn hộ Studio hiện đại gần công viên",
                                  "description": "Studio hiện đại với đầy đủ tiện nghi gần công viên lớn.",
                                  "user": {
                                    "userId": "user-456e7890-e89b-12d3-a456-426614174001",
                                    "firstName": "Le",
                                    "lastName": "Thi D",
                                    "email": "user2@example.com",
                                    "contactPhoneNumber": "0909876543",
                                    "contactPhoneVerified": true
                                  },
                                  "postDate": "2025-09-05T14:30:00",
                                  "expiryDate": "2025-12-31T23:59:59",
                                  "listingType": "RENT",
                                  "verified": true,
                                  "isVerify": false,
                                  "expired": false,
                                  "vipType": "SILVER",
                                  "categoryId": 11,
                                  "productType": "STUDIO",
                                  "price": 700.00,
                                  "priceUnit": "MONTH",
                                  "address": {
                                    "addressId": 502,
                                    "fullAddress": "456 Lê Lợi, Phường Bến Thành, Quận 1, Thành Phố Hồ Chí Minh",
                                    "fullNewAddress": "456 Lê Lợi, Phường Bến Thành, Thành Phố Hồ Chí Minh",
                                    "latitude": 10.7756,
                                    "longitude": 106.7019,
                                    "addressType": "OLD",
                                    "legacyProvinceId": 79,
                                    "legacyProvinceName": "Thành Phố Hồ Chí Minh",
                                    "legacyDistrictId": 762,
                                    "legacyDistrictName": "Quận 1",
                                    "legacyWardId": 26700,
                                    "legacyWardName": "Phường Bến Thành",
                                    "legacyStreet": "Lê Lợi",
                                    "newProvinceCode": "79",
                                    "newProvinceName": "Thành Phố Hồ Chí Minh",
                                    "newWardCode": "26734",
                                    "newWardName": "Phường Bến Thành",
                                    "newStreet": "Lê Lợi"
                                  },
                                  "area": 35.0,
                                  "bedrooms": 0,
                                  "bathrooms": 1,
                                  "direction": "SOUTH",
                                  "furnishing": "FULLY_FURNISHED",
                                  "roomCapacity": 2,
                                  "amenities": [
                                    {
                                      "amenityId": 1,
                                      "name": "Điều hòa",
                                      "icon": "ac-icon",
                                      "description": "Máy điều hòa không khí",
                                      "category": "BASIC",
                                      "isActive": true
                                    },
                                    {
                                      "amenityId": 5,
                                      "name": "WiFi miễn phí",
                                      "icon": "wifi-icon",
                                      "description": "Kết nối internet tốc độ cao",
                                      "category": "CONVENIENCE",
                                      "isActive": true
                                    }
                                  ],
                                  "createdAt": "2025-09-05T14:30:00",
                                  "updatedAt": "2025-09-05T14:30:00"
                                }
                              ]
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<List<ListingResponse>> getListings(
        @RequestParam(value = "ids", required = false) Set<Long> ids,
        @Parameter(description = "Page number (1-based indexing)", example = "1")
        @RequestParam(value = "page", defaultValue = "1") int page,
        @Parameter(description = "Number of items per page", example = "20")
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        if (ids != null && !ids.isEmpty()) {
            List<ListingResponse> responses = listingService.getListingsByIds(ids);
            return ApiResponse.<List<ListingResponse>>builder().data(responses).build();
        }
        List<ListingResponse> responses = listingService.getListings(page, size);
        return ApiResponse.<List<ListingResponse>>builder().data(responses).build();
    }

    @Operation(
        summary = "Update a listing",
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingRequest.class),
                examples = @ExampleObject(
                    name = "Update Listing Example",
                    value = """
                        {
                          "title": "Căn hộ 2 phòng ngủ đã cập nhật với ảnh mới",
                          "description": "Hiện bao gồm bếp được cải tạo và phòng tắm đã nâng cấp.",
                          "userId": 42,
                          "expiryDate": "2026-01-31T23:59:59",
                          "listingType": "RENT",
                          "vipType": "VIP",
                          "categoryId": 10,
                          "productType": "APARTMENT",
                          "price": 1300.00,
                          "priceUnit": "MONTH",
                          "addressId": 501,
                          "area": 80.0,
                          "bedrooms": 2,
                          "bathrooms": 1,
                          "direction": "NORTH",
                          "furnishing": "FULLY_FURNISHED",
                          "roomCapacity": 4,
                          "amenityIds": [1, 2, 3]
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing updated",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found",
                        value = """
                            {
                              "code": "404001",
                              "message": "LISTING_NOT_FOUND",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    @PutMapping("/{id}")
    public ApiResponse<ListingResponse> updateListing(@PathVariable Long id, @RequestBody ListingRequest request) {
        ListingResponse response = listingService.updateListing(id, request);
        return ApiResponse.<ListingResponse>builder().data(response).build();
    }

    @Operation(
        summary = "Delete a listing",
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing deleted",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Success",
                        value = """
                            { "code": "999999", "message": null, "data": null }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found",
                        value = """
                            {
                              "code": "404001",
                              "message": "LISTING_NOT_FOUND",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteListing(@PathVariable Long id) {
        listingService.deleteListing(id);
        return ApiResponse.<Void>builder().build();
    }

    /**
     * Create VIP or Premium listing with quota check and payment flow
     * POST /v1/listings/vip
     */
    @PostMapping("/vip")
    @Operation(
        summary = "Create VIP or Premium listing",
        description = """
            Creates a VIP or Premium listing with dual payment model:
            1. If useMembershipQuota=true and user has quota: Creates listing immediately using quota
            2. If useMembershipQuota=false or no quota: Returns payment URL. After payment completion, user will be redirected to frontend. Verify payment status using GET /v1/payments/transactions/{txnRef}.

            Premium listings automatically create a shadow NORMAL listing for double visibility.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VipListingCreationRequest.class),
                examples = {
                    @ExampleObject(
                        name = "SILVER with Quota",
                        summary = "SILVER listing sử dụng quota từ membership - Nested legacy address",
                        value = """
                            {
                              "title": "Căn hộ cao cấp tại Quận Ba Đình",
                              "description": "Căn hộ 2 phòng ngủ đẹp, đầy đủ nội thất",
                              "listingType": "RENT",
                              "vipType": "SILVER",
                              "categoryId": 1,
                              "productType": "APARTMENT",
                              "price": 15000000,
                              "priceUnit": "MONTH",
                              "address": {
                                "legacy": {
                                  "provinceId": 1,
                                  "districtId": 1,
                                  "wardId": 1,
                                  "street": "45A Hoàng Diệu"
                                },
                                "latitude": 21.0285,
                                "longitude": 105.8542
                              },
                              "area": 80.5,
                              "bedrooms": 2,
                              "bathrooms": 2,
                              "direction": "SOUTH",
                              "furnishing": "FULLY_FURNISHED",
                              "useMembershipQuota": true,
                              "durationDays": 30
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "GOLD with Payment",
                        summary = "GOLD listing với thanh toán VNPay - Nested new address",
                        value = """
                            {
                              "title": "Biệt thự GOLD tại Quận 2",
                              "description": "Biệt thự 3 phòng ngủ sang trọng",
                              "listingType": "RENT",
                              "vipType": "GOLD",
                              "categoryId": 1,
                              "productType": "HOUSE",
                              "price": 35000000,
                              "priceUnit": "MONTH",
                              "address": {
                                "newAddress": {
                                  "provinceCode": "79",
                                  "wardCode": "26734",
                                  "street": "120 Nguyễn Văn Hưởng"
                                },
                                "latitude": 10.7769,
                                "longitude": 106.7009
                              },
                              "area": 200.0,
                              "bedrooms": 3,
                              "bathrooms": 3,
                              "direction": "EAST",
                              "furnishing": "FULLY_FURNISHED",
                              "useMembershipQuota": false,
                              "durationDays": 30,
                              "paymentProvider": "VNPAY"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "DIAMOND with Shadow",
                        summary = "DIAMOND listing (tự động tạo shadow NORMAL listing) - Nested legacy address",
                        value = """
                            {
                              "title": "Penthouse DIAMOND Landmark 81",
                              "description": "Penthouse cao cấp nhất với view toàn thành phố",
                              "listingType": "RENT",
                              "vipType": "DIAMOND",
                              "categoryId": 1,
                              "productType": "PENTHOUSE",
                              "price": 100000000,
                              "priceUnit": "MONTH",
                              "address": {
                                "legacy": {
                                  "provinceId": 79,
                                  "districtId": 20,
                                  "wardId": 50,
                                  "street": "720A Đường Tân Cảng"
                                },
                                "latitude": 10.7941,
                                "longitude": 106.7218
                              },
                              "area": 500.0,
                              "bedrooms": 5,
                              "bathrooms": 5,
                              "direction": "NORTHEAST",
                              "furnishing": "LUXURY_FURNISHED",
                              "roomCapacity": 10,
                              "useMembershipQuota": true,
                              "durationDays": 30
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing created or payment URL returned",
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "Listing Created (Quota)",
                            value = """
                                {
                                  "data": {
                                    "listingId": "550e8400-e29b-41d4-a716-446655440000",
                                    "title": "Căn hộ cao cấp tại Quận 1",
                                    "vipType": "VIP",
                                    "postSource": "QUOTA",
                                    "status": "ACTIVE"
                                  }
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Payment Required",
                            value = """
                                {
                                  "data": {
                                    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=...",
                                    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
                                    "orderInfo": "Đăng tin Premium - 30 ngày",
                                    "amount": 1800000
                                  }
                                }
                                """
                        )
                    }
                )
            )
        }
    )
    public ApiResponse<Object> createVipListing(@Valid @RequestBody VipListingCreationRequest request) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        request.setUserId(userId);
        Object response = listingService.createVipListing(request);
        return ApiResponse.builder().data(response).build();
    }

    /**
     * Get listing statistics by provinces (for home screen)
     * POST /v1/listings/stats/provinces
     */
    @PostMapping("/stats/provinces")
    @Operation(
        summary = "Lấy thống kê số lượng bài đăng theo tỉnh/thành phố",
        description = """
            ## MÔ TẢ
            API này được thiết kế cho **màn hình Home** - Frontend truyền danh sách tỉnh/thành phố
            và nhận về thống kê số lượng bài đăng của từng tỉnh.

            **Use Case**: Hiển thị 5 địa điểm (tỉnh/thành phố) với số lượng bài đăng trên màn Home.

            ---

            ## AUTHENTICATION
            - **API CÔNG KHAI** - KHÔNG CẦN authentication token
            - **KHÔNG CẦN userId** trong request body
            - Có thể gọi trực tiếp từ màn hình Home mà không cần đăng nhập

            ---

            ## REQUEST
            Truyền danh sách **provinceIds** (old structure) HOẶC **provinceCodes** (new structure).
            - Không cần truyền cả hai, chỉ cần 1 trong 2 (provinceIds hoặc provinceCodes)
            - Nếu muốn lọc chỉ bài verified, set `verifiedOnly: true`

            ---

            ## RESPONSE
            Trả về array thống kê cho từng tỉnh bao gồm:
            - Tên tỉnh/thành phố
            - Tổng số bài đăng (`totalListings`)
            - Số bài đăng verified (`verifiedListings`)
            - Số bài đăng VIP tier cao (`vipListings` - SILVER/GOLD/DIAMOND)

            ---

            ## GỢI Ý SỬ DỤNG

            **Ví dụ 1**: Lấy thống kê 5 tỉnh lớn (Hà Nội, TP.HCM, Đà Nẵng, Hải Phòng, Cần Thơ)
            ```json
            {
              "provinceIds": [1, 79, 48, 31, 92],
              "verifiedOnly": false
            }
            ```

            **Ví dụ 2**: Chỉ đếm bài đã verify (new structure)
            ```json
            {
              "provinceCodes": ["01", "79", "48", "31", "92"],
              "verifiedOnly": true
            }
            ```

            ---

            ## LƯU Ý QUAN TRỌNG
            - **KHÔNG CẦN userId** - API công khai, không yêu cầu authentication
            - **KHÔNG CẦN access token** - Gọi trực tiếp từ màn Home
            - Tự động loại trừ bài nháp (draft) và bài shadow
            - Tự động loại trừ bài hết hạn (expired)
            - Response được sắp xếp theo thứ tự trong request
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProvinceStatsRequest.class),
                examples = {
                    @ExampleObject(
                        name = "1. Top 5 tỉnh lớn - Old structure",
                        summary = "Hà Nội, TP.HCM, Đà Nẵng, Hải Phòng, Cần Thơ",
                        value = """
                            {
                              "provinceIds": [1, 79, 48, 31, 92],
                              "verifiedOnly": false,
                              "addressType": "OLD"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "2. Top 5 tỉnh - New structure (chỉ verified)",
                        summary = "Chỉ đếm bài đã verify",
                        value = """
                            {
                              "provinceCodes": ["01", "79", "48", "31", "92"],
                              "verifiedOnly": true,
                              "addressType": "NEW"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "3. Ba miền - North/Central/South",
                        summary = "Hà Nội (Bắc), Đà Nẵng (Trung), TP.HCM (Nam)",
                        value = """
                            {
                              "provinceIds": [1, 48, 79],
                              "verifiedOnly": false
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Thống kê thành công",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Province Statistics Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": [
                                {
                                  "provinceId": 1,
                                  "provinceCode": null,
                                  "provinceName": "Hà Nội",
                                  "totalListings": 1250,
                                  "verifiedListings": 980,
                                  "vipListings": 345
                                },
                                {
                                  "provinceId": 79,
                                  "provinceCode": null,
                                  "provinceName": "Thành phố Hồ Chí Minh",
                                  "totalListings": 2340,
                                  "verifiedListings": 1890,
                                  "vipListings": 678
                                },
                                {
                                  "provinceId": 48,
                                  "provinceCode": null,
                                  "provinceName": "Đà Nẵng",
                                  "totalListings": 680,
                                  "verifiedListings": 520,
                                  "vipListings": 156
                                },
                                {
                                  "provinceId": 31,
                                  "provinceCode": null,
                                  "provinceName": "Hải Phòng",
                                  "totalListings": 420,
                                  "verifiedListings": 310,
                                  "vipListings": 89
                                },
                                {
                                  "provinceId": 92,
                                  "provinceCode": null,
                                  "provinceName": "Cần Thơ",
                                  "totalListings": 280,
                                  "verifiedListings": 195,
                                  "vipListings": 52
                                }
                              ]
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<List<ProvinceListingStatsResponse>> getProvinceStats(
            @Valid @RequestBody ProvinceStatsRequest request) {
        List<ProvinceListingStatsResponse> stats = listingService.getProvinceStats(request);
        return ApiResponse.<List<ProvinceListingStatsResponse>>builder()
                .data(stats)
                .build();
    }

    /**
     * Check available posting quota for VIP and Premium listings
     * GET /v1/listings/quota-check
     */
    @GetMapping("/quota-check")
    @Operation(
        summary = "Check posting quota",
        description = "Check available VIP and Premium posting quota for current user. Returns all quotas if vipType not specified.",
        parameters = {
            @Parameter(
                name = "vipType",
                description = "Specific VIP type to check (VIP or PREMIUM). If not provided, returns all quotas.",
                required = false
            )
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Quota information retrieved",
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "All Quotas",
                            value = """
                                {
                                  "data": {
                                    "vipPosts": {
                                      "totalAvailable": 5,
                                      "totalUsed": 2,
                                      "totalGranted": 7
                                    },
                                    "premiumPosts": {
                                      "totalAvailable": 2,
                                      "totalUsed": 1,
                                      "totalGranted": 3
                                    },
                                    "boosts": {
                                      "totalAvailable": 10,
                                      "totalUsed": 3,
                                      "totalGranted": 13
                                    }
                                  }
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Specific Quota",
                            value = """
                                {
                                  "data": {
                                    "totalAvailable": 5,
                                    "totalUsed": 2,
                                    "totalGranted": 7
                                  }
                                }
                                """
                        )
                    }
                )
            )
        }
    )
    public ApiResponse<Object> checkPostingQuota(@RequestParam(required = false) String vipType) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        if (vipType != null) {
            BenefitType benefitType = switch (vipType.toUpperCase()) {
                case "SILVER" -> BenefitType.POST_SILVER;
                case "GOLD" -> BenefitType.POST_GOLD;
                case "DIAMOND" -> BenefitType.POST_DIAMOND;
                default -> BenefitType.POST_SILVER;
            };
            QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, benefitType);
            return ApiResponse.builder().data(quota).build();
        }

        // Return all VIP tier quotas
        QuotaStatusResponse silverQuota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_SILVER);
        QuotaStatusResponse goldQuota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_GOLD);
        QuotaStatusResponse diamondQuota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_DIAMOND);
        QuotaStatusResponse pushQuota = quotaService.checkQuotaAvailability(userId, BenefitType.PUSH);

        return ApiResponse.builder().data(java.util.Map.of(
                "silverPosts", silverQuota,
                "goldPosts", goldQuota,
                "diamondPosts", diamondQuota,
                "pushes", pushQuota
        )).build();
    }

    /**
     * Get listing by ID with admin verification information (Admin only)
     * GET /v1/listings/{id}/admin
     */
    @GetMapping("/{id}/admin")
    @Operation(
        summary = "Get listing with admin verification info (Admin only)",
        description = "Retrieves listing details including admin verification information and status. This endpoint is for administrators only.",
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true),
            @Parameter(name = "X-Admin-Id", description = "Admin ID from authentication header", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing found with admin verification info",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Listing with Admin Info Example",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "listingId": 123,
                                "title": "Căn hộ 2 phòng ngủ ấm cúng trung tâm thành phố",
                                "description": "Căn hộ rộng rãi 2 phòng ngủ có ban công và tầm nhìn thành phố.",
                                "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                "postDate": "2025-09-01T10:00:00",
                                "expiryDate": "2025-12-31T23:59:59",
                                "listingType": "RENT",
                                "verified": true,
                                "isVerify": false,
                                "expired": false,
                                "vipType": "NORMAL",
                                "categoryId": 10,
                                "productType": "APARTMENT",
                                "price": 1200.00,
                                "priceUnit": "MONTH",
                                "addressId": 501,
                                "area": 78.5,
                                "bedrooms": 2,
                                "bathrooms": 1,
                                "direction": "NORTHEAST",
                                "furnishing": "SEMI_FURNISHED",
                                "roomCapacity": 4,
                                "amenities": [
                                  {
                                    "amenityId": 1,
                                    "name": "Điều hòa",
                                    "icon": "ac-icon",
                                    "description": "Máy điều hòa không khí",
                                    "category": "BASIC",
                                    "isActive": true
                                  },
                                  {
                                    "amenityId": 3,
                                    "name": "Máy giặt",
                                    "icon": "washing-machine-icon",
                                    "description": "Máy giặt tự động",
                                    "category": "BASIC",
                                    "isActive": true
                                  },
                                  {
                                    "amenityId": 5,
                                    "name": "WiFi miễn phí",
                                    "icon": "wifi-icon",
                                    "description": "Kết nối internet tốc độ cao",
                                    "category": "CONVENIENCE",
                                    "isActive": true
                                  }
                                ],
                                "media": [
                                  {
                                    "mediaId": 1,
                                    "listingId": 123,
                                    "mediaType": "IMAGE",
                                    "url": "https://placehold.co/400x300/cccccc/969696.png?font=lato",
                                    "isPrimary": true,
                                    "sortOrder": 0,
                                    "status": "ACTIVE",
                                    "createdAt": "2025-09-01T10:00:00"
                                  },
                                  {
                                    "mediaId": 2,
                                    "listingId": 123,
                                    "mediaType": "IMAGE",
                                    "url": "https://placehold.co/400x300/cccccc/969696.png?font=lato",
                                    "isPrimary": false,
                                    "sortOrder": 1,
                                    "status": "ACTIVE",
                                    "createdAt": "2025-09-01T10:05:00"
                                  }
                                ],
                                "adminVerification": {
                                  "adminId": "admin-123e4567-e89b-12d3-a456-426614174000",
                                  "adminName": "Jane Smith",
                                  "adminEmail": "admin@smartrent.com",
                                  "verifiedAt": "2025-09-01T11:30:00",
                                  "verificationStatus": "APPROVED",
                                  "verificationNotes": "Verified property documents and ownership. All information is accurate."
                                },
                                "createdAt": "2025-09-01T10:00:00",
                                "updatedAt": "2025-09-01T11:30:00",
                                "updatedBy": 1
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found",
                        value = """
                            {
                              "code": "404001",
                              "message": "LISTING_NOT_FOUND",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<ListingResponseWithAdmin> getListingByIdWithAdmin(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Id") String adminId) {
        ListingResponseWithAdmin response = listingService.getListingByIdWithAdmin(id, adminId);
        return ApiResponse.<ListingResponseWithAdmin>builder().data(response).build();
    }

    /**
     * Get my listing detail with owner-specific information (Owner only)
     * GET /v1/listings/{id}/my-detail
     */
    @GetMapping("/{id}/my-detail")
    @Operation(
        summary = "Get my listing detail with owner information (Owner only)",
        description = """
            Retrieves detailed listing information with owner-specific data including:
            - Transaction details (postSource, transactionId, payment info)
            - All media (images/videos) attached to the listing
            - Complete address information
            - Listing statistics (views, contacts, etc.)
            - Verification notes and rejection reason (if any)

            This endpoint is only accessible by the listing owner.
            User authentication is required via JWT token.
            """,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing found with owner details",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "My Listing Detail Example",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "listingId": 123,
                                "title": "Modern 2BR Apartment in District 1",
                                "description": "Beautiful apartment with city view",
                                "user": {
                                  "userId": "user-uuid-123",
                                  "phoneCode": "+84",
                                  "phoneNumber": "912345678",
                                  "email": "owner@example.com",
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "contactPhoneNumber": "0912345678",
                                  "contactPhoneVerified": true
                                },
                                "postDate": "2025-01-15T10:30:00",
                                "expiryDate": "2025-02-14T10:30:00",
                                "listingType": "FOR_RENT",
                                "verified": true,
                                "isVerify": true,
                                "expired": false,
                                "isDraft": false,
                                "vipType": "GOLD",
                                "price": 15000000,
                                "priceUnit": "VND_PER_MONTH",
                                "postSource": "DIRECT_PAYMENT",
                                "transactionId": "txn-123-abc",
                                "isShadow": false,
                                "parentListingId": null,
                                "media": [
                                  {
                                    "mediaId": 1,
                                    "url": "https://storage.example.com/listing-123/image1.jpg",
                                    "mediaType": "IMAGE",
                                    "isPrimary": true,
                                    "sortOrder": 1
                                  }
                                ],
                                "address": {
                                  "addressId": 456,
                                  "fullAddress": "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, Thành Phố Hồ Chí Minh",
                                  "fullNewAddress": "123 Nguyễn Huệ, Phường Bến Nghé, Thành Phố Hồ Chí Minh",
                                  "latitude": 10.7769,
                                  "longitude": 106.7009,
                                  "addressType": "OLD",
                                  "legacyProvinceId": 79,
                                  "legacyProvinceName": "Thành Phố Hồ Chí Minh",
                                  "legacyDistrictId": 760,
                                  "legacyDistrictName": "Quận 1",
                                  "legacyWardId": 26734,
                                  "legacyWardName": "Phường Bến Nghé",
                                  "legacyStreet": "Nguyễn Huệ",
                                  "newProvinceCode": "79",
                                  "newProvinceName": "Thành Phố Hồ Chí Minh",
                                  "newWardCode": "26734",
                                  "newWardName": "Phường Bến Nghé",
                                  "newStreet": "Nguyễn Huệ"
                                },
                                "paymentInfo": {
                                  "provider": "VNPAY",
                                  "status": "SUCCESS",
                                  "paidAt": "2025-01-15T10:25:00",
                                  "amount": 1200000,
                                  "vipTierPurchased": "GOLD",
                                  "durationPurchased": 30
                                },
                                "statistics": {
                                  "viewCount": 1245,
                                  "contactCount": 23,
                                  "saveCount": 15,
                                  "reportCount": 0
                                },
                                "verificationNotes": "Listing approved - all information verified",
                                "rejectionReason": null
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "User not authenticated or not the owner",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Unauthorized",
                        value = """
                            {
                              "code": "401001",
                              "message": "UNAUTHORIZED - You don't have permission to view this listing's detail",
                              "data": null
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found",
                        value = """
                            {
                              "code": "404001",
                              "message": "LISTING_NOT_FOUND",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<ListingResponseForOwner> getMyListingDetail(@PathVariable Long id) {
        String userId = extractUserId();
        ListingResponseForOwner response = listingService.getMyListingDetail(id, userId);
        return ApiResponse.<ListingResponseForOwner>builder().data(response).build();
    }

    /**
     * Get my listings with owner-specific information (Owner only)
     * GET /v1/listings/my-listings
     */
    @GetMapping("/my-listings")
    @Operation(
        summary = "Get my listings with owner-specific information (Owner only)",
        description = """
            Retrieves paginated list of owner's listings with detailed owner-specific information including:
            - Transaction details (postSource, transactionId, payment info)
            - All media (images/videos) for each listing
            - Complete address information
            - Listing statistics (views, contacts, etc.)
            - Verification notes and rejection reason (if any)

            **Features:**
            - Full owner-specific data for each listing
            - Flexible filtering (verified, expired, isDraft, vipType, etc.)
            - Pagination support
            - Owner dashboard statistics (drafts, pending, active, expired, by VIP tier)

            **Use Cases:**
            - Owner dashboard to manage all listings
            - View detailed information about each owned listing
            - Filter by status, VIP type, etc.

            User authentication is required via JWT token.
            """,
        parameters = {
            @Parameter(name = "page", description = "Page number (0-indexed)", example = "0"),
            @Parameter(name = "size", description = "Page size (max 100)", example = "20"),
            @Parameter(name = "sortBy", description = "Sort field (postDate, price, area, createdAt, updatedAt)", example = "postDate"),
            @Parameter(name = "sortDirection", description = "Sort direction (ASC, DESC)", example = "DESC"),
            @Parameter(name = "verified", description = "Filter by verification status (true/false)"),
            @Parameter(name = "expired", description = "Filter by expiry status (true/false)"),
            @Parameter(name = "isDraft", description = "Filter by draft status (true/false)"),
            @Parameter(name = "vipType", description = "Filter by VIP type (NORMAL, SILVER, GOLD, DIAMOND)")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "My listings retrieved successfully with statistics",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "My Listings Example",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "listings": [
                                  {
                                    "listingId": 123,
                                    "title": "Modern 2BR Apartment in District 1",
                                    "user": {
                                      "userId": "user-uuid-123",
                                      "firstName": "John",
                                      "lastName": "Doe",
                                      "email": "owner@example.com",
                                      "contactPhoneNumber": "0912345678",
                                      "contactPhoneVerified": true
                                    },
                                    "verified": true,
                                    "vipType": "GOLD",
                                    "postSource": "DIRECT_PAYMENT",
                                    "transactionId": "txn-123-abc",
                                    "media": [
                                      {
                                        "mediaId": 1,
                                        "url": "https://storage.example.com/image1.jpg",
                                        "isPrimary": true
                                      }
                                    ],
                                    "address": {
                                      "addressId": 456,
                                      "fullAddress": "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, Thành Phố Hồ Chí Minh",
                                      "fullNewAddress": "123 Nguyễn Huệ, Phường Bến Nghé, Thành Phố Hồ Chí Minh",
                                      "latitude": 10.7769,
                                      "longitude": 106.7009,
                                      "addressType": "OLD",
                                      "legacyProvinceId": 79,
                                      "legacyProvinceName": "Thành Phố Hồ Chí Minh",
                                      "legacyDistrictId": 760,
                                      "legacyDistrictName": "Quận 1",
                                      "legacyWardId": 26734,
                                      "legacyWardName": "Phường Bến Nghé",
                                      "legacyStreet": "Nguyễn Huệ",
                                      "newProvinceCode": "79",
                                      "newProvinceName": "Thành Phố Hồ Chí Minh",
                                      "newWardCode": "26734",
                                      "newWardName": "Phường Bến Nghé",
                                      "newStreet": "Nguyễn Huệ"
                                    },
                                    "statistics": {
                                      "viewCount": 1245,
                                      "contactCount": 23
                                    }
                                  }
                                ],
                                "totalCount": 25,
                                "currentPage": 0,
                                "pageSize": 20,
                                "totalPages": 2,
                                "statistics": {
                                  "drafts": 3,
                                  "pendingVerification": 5,
                                  "active": 12,
                                  "expired": 5,
                                  "normalListings": 15,
                                  "silverListings": 5,
                                  "goldListings": 3,
                                  "diamondListings": 2
                                }
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "User not authenticated",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Unauthorized",
                        value = """
                            {
                              "code": "401001",
                              "message": "UNAUTHORIZED - User not authenticated",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<OwnerListingListResponse> getMyListings(@Valid ListingFilterRequest filter) {
        String userId = extractUserId();
        OwnerListingListResponse response = listingService.getMyListings(filter, userId);
        return ApiResponse.<OwnerListingListResponse>builder().data(response).build();
    }

    /**
     * Get all listings for admin with pagination and filters (Admin only)
     * GET /v1/listings/admin/list
     */
    @GetMapping("/admin/list")
    @Operation(
        summary = "Get all listings for admin with pagination (Admin only)",
        description = """
            Retrieves paginated list of all listings with admin-specific information and comprehensive statistics.
            This endpoint is for administrators only.

            **Features:**
            - Complete listing data with admin verification info
            - Flexible filtering (category, province, VIP type, verification status, etc.)
            - Pagination support
            - Dashboard statistics (pending, verified, expired, by VIP tier, etc.)

            **Use Cases:**
            - Admin dashboard listing management
            - Verification queue management
            - Analytics and reporting
            """,
        parameters = {
            @Parameter(name = "X-Admin-Id", description = "Admin ID from authentication header", required = true),
            @Parameter(name = "page", description = "Page number (0-indexed)", example = "0"),
            @Parameter(name = "size", description = "Page size (max 100)", example = "20"),
            @Parameter(name = "sortBy", description = "Sort field (postDate, price, area, createdAt, updatedAt)", example = "postDate"),
            @Parameter(name = "sortDirection", description = "Sort direction (ASC, DESC)", example = "DESC"),
            @Parameter(name = "categoryId", description = "Filter by category ID", example = "1"),
            @Parameter(name = "provinceId", description = "Filter by province ID (old structure)", example = "79"),
            @Parameter(name = "provinceCode", description = "Filter by province code (new structure)", example = "79"),
            @Parameter(name = "vipType", description = "Filter by VIP type (NORMAL, SILVER, GOLD, DIAMOND)"),
            @Parameter(name = "verified", description = "Filter by verification status (true/false)"),
            @Parameter(name = "expired", description = "Filter by expiry status (true/false)"),
            @Parameter(name = "isDraft", description = "Filter by draft status (true/false)")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listings retrieved successfully with statistics",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Admin Listing List Example",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "listings": [
                                  {
                                    "listingId": 123,
                                    "title": "Modern 2BR Apartment",
                                    "userId": "user-uuid-123",
                                    "verified": false,
                                    "isVerify": true,
                                    "vipType": "GOLD",
                                    "adminVerification": {
                                      "adminId": "admin-uuid-456",
                                      "adminName": "John Admin",
                                      "verificationStatus": "PENDING",
                                      "verificationNotes": null
                                    }
                                  }
                                ],
                                "totalCount": 150,
                                "currentPage": 0,
                                "pageSize": 20,
                                "totalPages": 8,
                                "statistics": {
                                  "pendingVerification": 45,
                                  "verified": 1250,
                                  "expired": 180,
                                  "drafts": 23,
                                  "shadows": 89,
                                  "normalListings": 800,
                                  "silverListings": 200,
                                  "goldListings": 150,
                                  "diamondListings": 100
                                }
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Admin not authenticated or not authorized",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Unauthorized",
                        value = """
                            {
                              "code": "401001",
                              "message": "UNAUTHORIZED - Admin not found",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<AdminListingListResponse> getAllListingsForAdmin(
            @RequestHeader("X-Admin-Id") String adminId,
            @Valid ListingFilterRequest filter) {
        AdminListingListResponse response = listingService.getAllListingsForAdmin(filter, adminId);
        return ApiResponse.<AdminListingListResponse>builder().data(response).build();
    }

    // ============ DRAFT MANAGEMENT ENDPOINTS ============

    /**
     * Update draft listing (auto-save)
     * PATCH /v1/listings/{id}/draft
     */
    @PostMapping("/{id}/draft")
    @Operation(
        summary = "Update draft listing (auto-save)",
        description = """
            Update draft listing with partial data. All fields are optional.
            This endpoint is used for auto-saving draft listings during creation.

            - No validation for required fields (validation happens on publish)
            - Automatically sets isDraft=true
            - Only the owner can update their draft

            **Use case**: Auto-save every 30 seconds when user is creating a listing
            """,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true, example = "123")
        }
    )
    public ApiResponse<ListingResponse> updateDraft(
            @PathVariable Long id,
            @RequestBody ListingDraftUpdateRequest request) {
        String userId = extractUserId();
        ListingResponse response = listingService.updateDraft(id, request, userId);
        return ApiResponse.<ListingResponse>builder().data(response).build();
    }

    /**
     * Get all draft listings for current user
     * GET /v1/listings/my-drafts
     */
    @GetMapping("/my-drafts")
    @Operation(
        summary = "Get my draft listings",
        description = """
            Get all draft listings for the authenticated user.
            Returns drafts sorted by last updated time (newest first).

            **Use case**: Display list of drafts for user to continue editing
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved draft listings",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = List.class),
                    examples = @ExampleObject(
                        name = "Draft Listings Example",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": [
                                {
                                  "listingId": 123,
                                  "title": "Căn hộ 2PN Q7",
                                  "description": "Chưa hoàn thiện mô tả...",
                                  "isDraft": true,
                                  "price": 5000000,
                                  "priceUnit": "MONTH",
                                  "updatedAt": "2025-11-29T10:30:00",
                                  "media": [
                                    {
                                      "mediaId": 1,
                                      "url": "https://example.com/image1.jpg",
                                      "isPrimary": true
                                    }
                                  ]
                                }
                              ]
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<List<com.smartrent.dto.response.ListingResponseForOwner>> getMyDrafts() {
        String userId = extractUserId();
        List<com.smartrent.dto.response.ListingResponseForOwner> response = listingService.getMyDrafts(userId);
        return ApiResponse.<List<com.smartrent.dto.response.ListingResponseForOwner>>builder().data(response).build();
    }

    /**
     * Publish draft listing
     * POST /v1/listings/{id}/publish
     */
    @PostMapping("/{id}/publish")
    @Operation(
        summary = "Publish draft listing",
        description = """
            Publish a draft listing after validating all required fields.

            **Validation**:
            - Validates all required fields (title, description, price, address, etc.)
            - Sets isDraft=false
            - Sets postDate to current time
            - Sets expiryDate if not already set

            **Required fields**:
            - title, description
            - listingType, productType
            - price, priceUnit
            - address
            - categoryId

            **Use case**: User clicks "Publish" button after completing the listing
            """,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true, example = "123")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully published draft listing",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Missing required fields or listing is not a draft",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Validation Error",
                        value = """
                            {
                              "code": "400001",
                              "message": "Cannot publish listing. Missing required fields: title, description, price",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<ListingResponse> publishDraft(@PathVariable Long id) {
        String userId = extractUserId();
        ListingResponse response = listingService.publishDraft(id, userId);
        return ApiResponse.<ListingResponse>builder().data(response).build();
    }

    /**
     * Delete draft listing
     * DELETE /v1/listings/{id}/draft
     */
    @DeleteMapping("/{id}/draft")
    @Operation(
        summary = "Delete draft listing",
        description = """
            Delete a draft listing. Only draft listings can be deleted.
            Published listings cannot be deleted through this endpoint.

            **Use case**: User wants to discard a draft
            """,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true, example = "123")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully deleted draft listing",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Success",
                        value = """
                            {
                              "code": "999999",
                              "message": "Draft deleted successfully",
                              "data": null
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Listing is not a draft",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not a draft",
                        value = """
                            {
                              "code": "400001",
                              "message": "Only draft listings can be deleted. Published listings cannot be deleted.",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<Void> deleteDraft(@PathVariable Long id) {
        String userId = extractUserId();
        listingService.deleteDraft(id, userId);
        return ApiResponse.<Void>builder().message("Draft deleted successfully").build();
    }

    /**
     * Extract user ID from authentication token
     * @return User ID from JWT subject claim
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return authentication.getName();
    }
}