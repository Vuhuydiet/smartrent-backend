package com.smartrent.controller;

import com.smartrent.dto.request.CategoryStatsRequest;
import com.smartrent.dto.request.DraftListingRequest;
import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.MapBoundsRequest;
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
            - benefitIds (required when useMembershipQuota=true, vipType is inferred from benefit type)
            - paymentProvider (VNPAY, etc.)

            **Note:** For draft listings, use POST /v1/listings/draft endpoint instead.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingCreationRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Listing Creation with Payment",
                        summary = "Create listing with payment (no membership quota)",
                        value = """
                            {
                              "title": "Căn hộ 2 phòng ngủ ấm cúng trung tâm thành phố",
                              "description": "Căn hộ rộng rãi 2 phòng ngủ có ban công và tầm nhìn thành phố.",
                              "listingType": "RENT",
                              "vipType": "SILVER",
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
                              "durationDays": 30,
                              "waterPrice": "NEGOTIABLE",
                              "electricityPrice": "SET_BY_OWNER",
                              "internetPrice": "PROVIDER_RATE",
                              "useMembershipQuota": false,
                              "paymentProvider": "VNPAY"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Listing with Membership Quota",
                        summary = "Create VIP listing using membership quota - vipType is inferred from benefitIds",
                        value = """
                            {
                              "title": "Căn hộ cao cấp 3 phòng ngủ",
                              "description": "Căn hộ cao cấp với đầy đủ tiện nghi, view đẹp. VIP type will be inferred from the benefit type (POST_SILVER->SILVER, POST_GOLD->GOLD, POST_DIAMOND->DIAMOND)",
                              "listingType": "RENT",
                              "categoryId": 1,
                              "productType": "APARTMENT",
                              "price": 25000000,
                              "priceUnit": "MONTH",
                              "address": {
                                "legacy": {
                                  "provinceId": 1,
                                  "districtId": 5,
                                  "wardId": 20,
                                  "street": "456 Lê Lợi"
                                },
                                "latitude": 21.0285,
                                "longitude": 105.8542
                              },
                              "area": 120.0,
                              "bedrooms": 3,
                              "bathrooms": 2,
                              "direction": "SOUTH",
                              "furnishing": "FULLY_FURNISHED",
                              "roomCapacity": 6,
                              "amenityIds": [1, 2, 3, 4, 5],
                              "mediaIds": [201, 202, 203],
                              "useMembershipQuota": true,
                              "benefitIds": [101, 102]
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
        summary = "Create draft listing",
        description = """
            Create a draft listing with all optional fields.
            Used for auto-save functionality during listing creation.

            **All fields are optional** - you can save partial data at any time.

            The draft can later be:
            - Updated via PATCH /v1/listings/{id}/draft
            - Published via POST /v1/listings/{id}/publish
            - Deleted via DELETE /v1/listings/{id}/draft
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DraftListingRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Minimal Draft",
                        summary = "Save draft with minimal data",
                        value = """
                            {
                              "title": "Căn hộ 2 phòng ngủ"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Partial Draft",
                        summary = "Save draft with partial data",
                        value = """
                            {
                              "title": "Căn hộ 2 phòng ngủ",
                              "description": "Căn hộ đang cập nhật thông tin...",
                              "listingType": "RENT",
                              "categoryId": 1,
                              "price": 12000000
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Complete Draft (Legacy Address)",
                        summary = "Save draft with all data using legacy address structure (63 provinces, 3-tier)",
                        value = """
                            {
                              "title": "Căn hộ cao cấp 3 phòng ngủ",
                              "description": "Căn hộ cao cấp với đầy đủ tiện nghi",
                              "listingType": "RENT",
                              "vipType": "GOLD",
                              "categoryId": 1,
                              "productType": "APARTMENT",
                              "price": 25000000,
                              "priceUnit": "MONTH",
                              "address": {
                                "legacy": {
                                  "provinceId": 1,
                                  "districtId": 5,
                                  "wardId": 20,
                                  "street": "456 Lê Lợi"
                                },
                                "latitude": 21.0285,
                                "longitude": 105.8542
                              },
                              "area": 120.0,
                              "bedrooms": 3,
                              "bathrooms": 2,
                              "direction": "SOUTH",
                              "furnishing": "FULLY_FURNISHED",
                              "roomCapacity": 6,
                              "amenityIds": [1, 2, 3, 4, 5],
                              "mediaIds": [201, 202, 203]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Complete Draft (New Address)",
                        summary = "Save draft with all data using new address structure (34 provinces, 2-tier)",
                        value = """
                            {
                              "title": "Căn hộ hiện đại 2 phòng ngủ",
                              "description": "Căn hộ hiện đại tại trung tâm thành phố",
                              "listingType": "RENT",
                              "vipType": "SILVER",
                              "categoryId": 1,
                              "productType": "APARTMENT",
                              "price": 15000000,
                              "priceUnit": "MONTH",
                              "address": {
                                "newAddress": {
                                  "provinceCode": "79",
                                  "wardCode": "27001",
                                  "street": "Nguyễn Văn Linh"
                                },
                                "latitude": 10.7412,
                                "longitude": 106.7220
                              },
                              "area": 85.0,
                              "bedrooms": 2,
                              "bathrooms": 2,
                              "direction": "EAST",
                              "furnishing": "SEMI_FURNISHED",
                              "roomCapacity": 4,
                              "amenityIds": [1, 2, 3],
                              "mediaIds": [301, 302]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Complete Draft (Both Addresses)",
                        summary = "Save draft with BOTH legacy AND new address structures - both will be saved to database",
                        value = """
                            {
                              "title": "Nhà mặt tiền 3 tầng",
                              "description": "Nhà mặt tiền đường lớn, kinh doanh tốt",
                              "listingType": "SALE",
                              "vipType": "GOLD",
                              "categoryId": 2,
                              "productType": "HOUSE",
                              "price": 8500000000,
                              "priceUnit": "TOTAL",
                              "address": {
                                "legacy": {
                                  "provinceId": 1,
                                  "districtId": 5,
                                  "wardId": 20,
                                  "street": "Lê Văn Sỹ"
                                },
                                "newAddress": {
                                  "provinceCode": "79",
                                  "wardCode": "27004",
                                  "street": "Lê Văn Sỹ"
                                },
                                "latitude": 10.7994,
                                "longitude": 106.6711
                              },
                              "area": 180.0,
                              "bedrooms": 5,
                              "bathrooms": 4,
                              "direction": "EAST",
                              "furnishing": "BASIC",
                              "roomCapacity": 8,
                              "amenityIds": [1, 2, 5, 8],
                              "mediaIds": [401, 402, 403, 404]
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Draft listing created successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Draft Created",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "draftId": 456,
                                "userId": "user-uuid-123",
                                "title": "Căn hộ 2 phòng ngủ ấm cúng",
                                "description": "Căn hộ rộng rãi với đầy đủ tiện nghi",
                                "listingType": "RENT",
                                "vipType": "NORMAL",
                                "categoryId": 10,
                                "productType": "APARTMENT",
                                "price": 5000000,
                                "priceUnit": "MONTH",
                                "address": {
                                  "addressType": "NEW",
                                  "newProvinceCode": "79",
                                  "newWardCode": "27001",
                                  "newStreet": "Nguyễn Văn Linh",
                                  "streetId": null,
                                  "projectId": null,
                                  "latitude": 10.7412,
                                  "longitude": 106.7220
                                },
                                "area": 75.5,
                                "bedrooms": 2,
                                "bathrooms": 2,
                                "direction": "SOUTH",
                                "furnishing": "FULLY_FURNISHED",
                                "roomCapacity": 4,
                                "waterPrice": "FREE",
                                "electricityPrice": "3500",
                                "internetPrice": "INCLUDED",
                                "serviceFee": "100000",
                                "amenities": [
                                  {
                                    "amenityId": 1,
                                    "name": "Wifi",
                                    "icon": "wifi"
                                  },
                                  {
                                    "amenityId": 2,
                                    "name": "Parking",
                                    "icon": "parking"
                                  }
                                ],
                                "media": [
                                  {
                                    "mediaId": 201,
                                    "url": "https://pub-xxx.r2.dev/media/...",
                                    "mediaType": "IMAGE",
                                    "isPrimary": true,
                                    "sortOrder": 1
                                  }
                                ],
                                "createdAt": "2024-01-15T10:30:00",
                                "updatedAt": "2024-01-15T10:30:00"
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request data",
                content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or missing authentication",
                content = @Content(mediaType = "application/json")
            )
        }
    )
    @PostMapping("/draft")
    public ApiResponse<DraftListingResponse> createDraftListing(@RequestBody DraftListingRequest request) {
        String userId = extractUserId();
        request.setUserId(userId);
        DraftListingResponse response = listingService.createDraftListing(request);
        return ApiResponse.<DraftListingResponse>builder().data(response).build();
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
                                  "contactPhoneVerified": true,
                                  "avatarUrl": "https://lh3.googleusercontent.com/a/example"
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
            - `sortBy`: DEFAULT (VIP tier + postDate), PRICE_ASC (giá tăng dần), PRICE_DESC (giá giảm dần), NEWEST (mới nhất), OLDEST (cũ nhất)
            - `sortDirection`: ASC, DESC (mặc định DESC)

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
            {"verified": true, "excludeExpired": true, "page": 0, "size": 20, "sortBy": "DEFAULT", "sortDirection": "DESC"}
            ```

            **Use Case 2: Lọc căn hộ cho thuê tại Hà Nội**
            ```json
            {"provinceId": "1", "isLegacy": true, "listingType": "RENT", "productType": "APARTMENT", "verified": true, "hasMedia": true, "page": 0, "size": 20}
            ```

            **Use Case 3: Tìm nhà giá 5-15 triệu/tháng, 2-3 phòng ngủ**
            ```json
            {"provinceId": "1", "isLegacy": true, "minPrice": 5000000, "maxPrice": 15000000, "priceUnit": "MONTH", "minBedrooms": 2, "maxBedrooms": 3, "verified": true, "sortBy": "PRICE_ASC"}
            ```

            **Use Case 4: Bài đăng đang giảm giá**
            ```json
            {"hasPriceReduction": true, "minPriceReductionPercent": 10, "priceChangedWithinDays": 30, "verified": true, "hasMedia": true}
            ```

            **Use Case 5: Tìm nhà gần vị trí hiện tại (GPS)**
            ```json
            {"latitude": 21.0285, "longitude": 105.8542, "radiusKm": 5.0, "verified": true}
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
            {"postedWithinDays": 7, "verified": true, "hasMedia": true, "sortBy": "NEWEST", "sortDirection": "DESC"}
            ```

            **Use Case 9: Chỉ lấy bài VIP (GOLD/DIAMOND)**
            ```json
            {"vipType": "GOLD", "verified": true, "sortBy": "NEWEST", "sortDirection": "DESC"}
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
            {"userId": "user-123", "listingStatus": "EXPIRING_SOON", "sortBy": "DEFAULT", "sortDirection": "ASC"}
            ```

            **Use Case 14: Owner - Listings đang chờ duyệt**
            ```json
            {"userId": "user-123", "listingStatus": "IN_REVIEW", "sortBy": "DEFAULT", "sortDirection": "DESC"}
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
                              "listingStatus": "DISPLAYING",
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
                                      "contactPhoneVerified": true,
                                      "avatarUrl": "https://lh3.googleusercontent.com/a/example"
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

    @PostMapping("/map-bounds")
    @Operation(
        summary = "[PUBLIC API] Get listings within map bounds for interactive map display",
        description = """
            **PUBLIC API - Không cần authentication**

            API để lấy danh sách bài đăng trong vùng hiển thị trên bản đồ (map bounds).
            Được thiết kế cho tính năng bản đồ tương tác (interactive map).

            ## TỌA ĐỘ VÀ ZOOM

            - `neLat`, `neLng`: Vĩ độ và kinh độ góc Đông Bắc
            - `swLat`, `swLng`: Vĩ độ và kinh độ góc Tây Nam
            - `zoom`: Mức zoom hiện tại của bản đồ (1-22)
              - Zoom thấp (1-10): View toàn quốc/vùng
              - Zoom trung bình (11-15): View tỉnh/quận
              - Zoom cao (16-22): View phố/khu vực nhỏ

            ## CÁC THAM SỐ BỔ SUNG

            - `limit`: Giới hạn số lượng kết quả (mặc định 100, tối đa 500)
            - `verifiedOnly`: Chỉ lấy bài đã verify (mặc định false)
            - `categoryId`: Lọc theo loại BĐS (tùy chọn)
            - `vipType`: Lọc theo VIP tier (tùy chọn)


            ## RESPONSE

            - `listings`: Danh sách bài đăng trong vùng
            - `totalCount`: Tổng số bài đăng tìm thấy
            - `returnedCount`: Số bài thực tế trả về (có thể < totalCount do limit)
            - `hasMore`: Có nhiều bài hơn không (totalCount > returnedCount)
            - `bounds`: Thông tin vùng đã query (echo lại request)

       
            ## LƯU Ý
            - **Tự động loại trừ**: Bài nháp, bài shadow, bài hết hạn
            - **Performance**: Limit cao (>300) có thể ảnh hưởng hiệu năng
            - **Tọa độ hợp lệ**: neLat > swLat, neLng > swLng
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
                    ),
                    @ExampleObject(
                        name = "Verified Only",
                        summary = "Chỉ lấy bài đã verify trong vùng",
                        value = """
                            {
                              "neLat": 10.823,
                              "neLng": 106.701,
                              "swLat": 10.705,
                              "swLng": 106.590,
                              "zoom": 14,
                              "limit": 100,
                              "verifiedOnly": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "With Filters",
                        summary = "Lọc theo category và VIP type",
                        value = """
                            {
                              "neLat": 10.823,
                              "neLng": 106.701,
                              "swLat": 10.705,
                              "swLng": 106.590,
                              "zoom": 14,
                              "limit": 150,
                              "verifiedOnly": true,
                              "categoryId": 1,
                              "vipType": "GOLD"
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
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Map Listings Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "listings": [
                                  {
                                    "listingId": 123,
                                    "title": "Căn hộ 2PN cao cấp Quận 1",
                                    "description": "Căn hộ đẹp view thành phố",
                                    "user": {
                                      "userId": "user-abc-123",
                                      "firstName": "Nguyen",
                                      "lastName": "Van A",
                                      "contactPhoneNumber": "0987654321",
                                      "contactPhoneVerified": true
                                    },
                                    "price": 15000000,
                                    "priceUnit": "MONTH",
                                    "address": {
                                      "addressId": 456,
                                      "fullAddress": "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM",
                                      "latitude": 10.7769,
                                      "longitude": 106.7009,
                                      "legacyProvinceName": "Thành Phố Hồ Chí Minh",
                                      "legacyDistrictName": "Quận 1",
                                      "legacyWardName": "Phường Bến Nghé"
                                    },
                                    "area": 85.0,
                                    "bedrooms": 2,
                                    "bathrooms": 2,
                                    "vipType": "GOLD",
                                    "listingType": "RENT",
                                    "productType": "APARTMENT",
                                    "verified": true,
                                    "postDate": "2025-12-15T10:00:00",
                                    "media": [
                                      {
                                        "mediaId": 1,
                                        "url": "https://storage.example.com/image1.jpg",
                                        "isPrimary": true
                                      }
                                    ]
                                  },
                                  {
                                    "listingId": 456,
                                    "title": "Penthouse DIAMOND Landmark 81",
                                    "description": "Penthouse siêu sang",
                                    "user": {
                                      "userId": "user-xyz-456",
                                      "firstName": "Tran",
                                      "lastName": "Thi B",
                                      "contactPhoneNumber": "0912345678",
                                      "contactPhoneVerified": true
                                    },
                                    "price": 100000000,
                                    "priceUnit": "MONTH",
                                    "address": {
                                      "addressId": 789,
                                      "fullAddress": "720A Đường Tân Cảng, Phường 25, Quận Bình Thạnh, TP.HCM",
                                      "latitude": 10.7941,
                                      "longitude": 106.7218,
                                      "legacyProvinceName": "Thành Phố Hồ Chí Minh",
                                      "legacyDistrictName": "Quận Bình Thạnh"
                                    },
                                    "area": 500.0,
                                    "bedrooms": 5,
                                    "bathrooms": 5,
                                    "vipType": "DIAMOND",
                                    "listingType": "RENT",
                                    "productType": "PENTHOUSE",
                                    "verified": true,
                                    "postDate": "2025-12-18T14:30:00",
                                    "media": [
                                      {
                                        "mediaId": 10,
                                        "url": "https://storage.example.com/penthouse1.jpg",
                                        "isPrimary": true
                                      }
                                    ]
                                  }
                                ],
                                "totalCount": 235,
                                "returnedCount": 100,
                                "hasMore": true,
                                "bounds": {
                                  "neLat": 10.823,
                                  "neLng": 106.701,
                                  "swLat": 10.705,
                                  "swLng": 106.590,
                                  "zoom": 14
                                }
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request - invalid coordinates or parameters",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Invalid Coordinates",
                        value = """
                            {
                              "code": "400001",
                              "message": "Invalid coordinates: neLat must be greater than swLat",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<MapListingsResponse> getListingsByMapBounds(
            @Valid @RequestBody MapBoundsRequest request) {
        MapListingsResponse response = listingService.getListingsByMapBounds(request);
        return ApiResponse.<MapListingsResponse>builder().data(response).build();
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
                                    "contactPhoneVerified": true,
                                    "avatarUrl": "https://lh3.googleusercontent.com/a/example"
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
                                    "contactPhoneVerified": true,
                                    "avatarUrl": null
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
     *
     * @deprecated Use POST /v1/listings instead. This endpoint will be removed in a future version.
     */
    @Deprecated
    @PostMapping("/vip")
    @Operation(
        summary = "[DEPRECATED] Create VIP or Premium listing - Use POST /v1/listings instead",
        description = """
            ⚠️ **DEPRECATED**: This endpoint is deprecated. Please use `POST /v1/listings` instead.

            The main `/v1/listings` endpoint now supports all listing types (NORMAL, SILVER, GOLD, DIAMOND)
            with the same payment and quota functionality.

            ---

            Creates a VIP or Premium listing with dual payment model:
            1. If useMembershipQuota=true and user has quota: Creates listing immediately using quota
            2. If useMembershipQuota=false or no quota: Returns payment URL. After payment completion, user will be redirected to frontend. Verify payment status using GET /v1/payments/transactions/{txnRef}.

            Premium listings automatically create a shadow NORMAL listing for double visibility.
            """,
        deprecated = true,
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
     * Get listing statistics by categories (for home screen)
     * POST /v1/listings/stats/categories
     * PUBLIC API - NO authentication required
     */
    @PostMapping("/stats/categories")
    @Operation(
        summary = "Lấy thống kê bài đăng theo categories (API công khai)",
        description = """
            API công khai để lấy thống kê số lượng bài đăng theo danh sách categories.
            Trả về số lượng tổng, đã verify, và VIP cho mỗi category.

            **Use cases:**
            - Home screen: Hiển thị số lượng bài đăng cho mỗi loại BĐS
            - Category page: Hiển thị tổng số bài đăng trong category
            - Search filters: Hiển thị số lượng kết quả cho mỗi category

            **Public API** - Không cần authentication
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Category statistics request với danh sách category IDs",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CategoryStatsRequest.class),
                examples = {
                    @ExampleObject(
                        name = "1. All categories",
                        summary = "Lấy thống kê cho tất cả loại BĐS",
                        value = """
                            {
                              "categoryIds": [1, 2, 3, 4, 5],
                              "verifiedOnly": false
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "2. Top categories (verified only)",
                        summary = "Chỉ đếm bài đã verify",
                        value = """
                            {
                              "categoryIds": [1, 2, 3],
                              "verifiedOnly": true
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
                        name = "Category Statistics Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": [
                                {
                                  "categoryId": 1,
                                  "categoryName": "Căn hộ/Chung cư",
                                  "categorySlug": "can-ho-chung-cu",
                                  "categoryIcon": "apartment",
                                  "totalListings": 3450,
                                  "verifiedListings": 2890,
                                  "vipListings": 1023
                                },
                                {
                                  "categoryId": 2,
                                  "categoryName": "Nhà nguyên căn",
                                  "categorySlug": "nha-nguyen-can",
                                  "categoryIcon": "house",
                                  "totalListings": 2180,
                                  "verifiedListings": 1750,
                                  "vipListings": 567
                                },
                                {
                                  "categoryId": 3,
                                  "categoryName": "Phòng trọ",
                                  "categorySlug": "phong-tro",
                                  "categoryIcon": "room",
                                  "totalListings": 5670,
                                  "verifiedListings": 4320,
                                  "vipListings": 892
                                },
                                {
                                  "categoryId": 4,
                                  "categoryName": "Văn phòng",
                                  "categorySlug": "van-phong",
                                  "categoryIcon": "office",
                                  "totalListings": 890,
                                  "verifiedListings": 720,
                                  "vipListings": 234
                                },
                                {
                                  "categoryId": 5,
                                  "categoryName": "Mặt bằng kinh doanh",
                                  "categorySlug": "mat-bang-kinh-doanh",
                                  "categoryIcon": "store",
                                  "totalListings": 1230,
                                  "verifiedListings": 980,
                                  "vipListings": 345
                                }
                              ]
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<List<CategoryListingStatsResponse>> getCategoryStats(
            @Valid @RequestBody CategoryStatsRequest request) {
        List<CategoryListingStatsResponse> stats = listingService.getCategoryStats(request);
        return ApiResponse.<List<CategoryListingStatsResponse>>builder()
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
                                "user": {
                                  "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "email": "john@example.com",
                                  "phoneCode": "+84",
                                  "phoneNumber": "912345678"
                                },
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
                                  "contactPhoneVerified": true,
                                  "avatarUrl": "https://lh3.googleusercontent.com/a/example"
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
     * POST /v1/listings/my-listings
     */
    @PostMapping("/my-listings")
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
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Comprehensive Filter Example",
                    summary = "Example showing all available filter options (use any combination)",
                    value = """
                        {
                          "page": 0,
                          "size": 20,
                          "sortBy": "DEFAULT",
                          "listingStatus": "EXPIRING_SOON",
                          "sortDirection": "DESC",
                          "verified": true,
                          "expired": false,
                          "isDraft": false,
                          "isVerify": true,
                          "vipType": "GOLD",
                          "categoryId": 1,
                          "provinceId": "79",
                          "minPrice": 1000000,
                          "maxPrice": 50000000,
                          "minArea": 20,
                          "maxArea": 100
                        }
                        """
                )
            )
        ),
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
                                      "contactPhoneVerified": true,
                                      "avatarUrl": "https://lh3.googleusercontent.com/a/example"
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
                                  "rejected": 2,
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
    public ApiResponse<OwnerListingListResponse> getMyListings(@Valid @RequestBody ListingFilterRequest filter) {
        String userId = extractUserId();
        OwnerListingListResponse response = listingService.getMyListings(filter, userId);
        return ApiResponse.<OwnerListingListResponse>builder().data(response).build();
    }

    /**
     * Get all listings for admin with pagination and filters (Admin only)
     * POST /v1/listings/admin/list
     */
    @PostMapping("/admin/list")
    @Operation(
        summary = "Get all listings for admin with filters and statistics",
        description = """
            Admin endpoint to retrieve listings with verification info and dashboard statistics.

            **Key Features:**
            - Filter by verification status, VIP type, location, price, etc.
            - Get statistics: pending/verified/expired counts, VIP tier breakdown
            - Admin verification info always includes verificationStatus (PENDING/APPROVED/REJECTED)

            **Common Filters:**
            - `verified` + `isVerify`: Filter by admin verification status
            - `vipType`: NORMAL, SILVER, GOLD, DIAMOND
            - `listingStatus`: IN_REVIEW, DISPLAYING, EXPIRED, etc.
            - `categoryId`, `provinceId`, `userId`: Filter by category/location/owner
            """,
        parameters = {
            @Parameter(name = "X-Admin-Id", description = "Admin ID", required = true)
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Pending listings",
                        summary = "Get listings waiting for verification",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "verified": false,
                              "isVerify": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "All filters",
                        summary = "All available filters (use any combination)",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "sortBy": "DEFAULT",
                              "sortDirection": "DESC",
                              "verified": true,
                              "isVerify": true,
                              "expired": false,
                              "vipType": "GOLD",
                              "categoryId": 1,
                              "provinceId": "79",
                              "userId": "user-uuid-123",
                              "listingType": "RENT",
                              "productType": "APARTMENT",
                              "minPrice": 1000000,
                              "maxPrice": 50000000
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Success",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "code": "999999",
                              "data": {
                                "listings": [
                                  {
                                    "listingId": 123,
                                    "title": "Modern 2BR Apartment",
                                    "user": {
                                      "userId": "user-uuid-123",
                                      "firstName": "John",
                                      "lastName": "Doe",
                                      "email": "john@example.com",
                                      "phoneCode": "+84",
                                      "phoneNumber": "912345678"
                                    },
                                    "verified": false,
                                    "isVerify": true,
                                    "vipType": "GOLD",
                                    "adminVerification": {
                                      "adminId": null,
                                      "adminName": null,
                                      "verificationStatus": "PENDING",
                                      "verificationNotes": null
                                    }
                                  }
                                ],
                                "totalCount": 150,
                                "currentPage": 1,
                                "pageSize": 20,
                                "totalPages": 8,
                                "statistics": {
                                  "pendingVerification": 45,
                                  "verified": 1250,
                                  "expired": 180,
                                  "rejected": 35,
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
            )
        }
    )
    public ApiResponse<AdminListingListResponse> getAllListingsForAdmin(
            @RequestHeader("X-Admin-Id") String adminId,
            @Valid @RequestBody ListingFilterRequest filter) {
        // Mark this as an admin request so the specification doesn't apply public filters
        filter.setIsAdminRequest(true);
        AdminListingListResponse response = listingService.getAllListingsForAdmin(filter, adminId);
        return ApiResponse.<AdminListingListResponse>builder().data(response).build();
    }

    // ============ DRAFT MANAGEMENT ENDPOINTS ============

    /**
     * Update draft listing (auto-save)
     * PATCH /v1/listings/draft/{draftId}
     */
    @PostMapping("/draft/{draftId}")
    @Operation(
        summary = "Update draft listing (auto-save)",
        description = """
            Update draft listing with partial data. All fields are optional.
            This endpoint is used for auto-saving draft listings during creation.

            - No validation for required fields (validation happens on publish)
            - Only the owner can update their draft
            - Drafts are stored in a separate table from published listings

            **Use case**: Auto-save every 30 seconds when user is creating a listing
            """,
        parameters = {
            @Parameter(name = "draftId", description = "Draft ID", required = true, example = "123")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Partial draft data to update (all fields optional)",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DraftListingRequest.class),
                examples = @ExampleObject(
                    name = "Partial Update Example",
                    summary = "Update only specific fields",
                    value = """
                        {
                          "title": "Updated title",
                          "price": 6000000,
                          "bedrooms": 3
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Draft updated successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Updated Draft Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "draftId": 456,
                                "userId": "user-uuid-123",
                                "title": "Updated title",
                                "description": "Căn hộ rộng rãi với đầy đủ tiện nghi",
                                "listingType": "RENT",
                                "vipType": "NORMAL",
                                "categoryId": 10,
                                "productType": "APARTMENT",
                                "price": 6000000,
                                "priceUnit": "MONTH",
                                "address": {
                                  "addressType": "NEW",
                                  "newProvinceCode": "79",
                                  "newWardCode": "27001",
                                  "newStreet": "Nguyễn Văn Linh",
                                  "latitude": 10.7412,
                                  "longitude": 106.7220
                                },
                                "area": 75.5,
                                "bedrooms": 3,
                                "bathrooms": 2,
                                "direction": "SOUTH",
                                "furnishing": "FULLY_FURNISHED",
                                "roomCapacity": 4,
                                "amenities": [
                                  {
                                    "amenityId": 1,
                                    "name": "Wifi",
                                    "icon": "wifi"
                                  }
                                ],
                                "media": [
                                  {
                                    "mediaId": 201,
                                    "url": "https://pub-xxx.r2.dev/media/...",
                                    "mediaType": "IMAGE",
                                    "isPrimary": true,
                                    "sortOrder": 1
                                  }
                                ],
                                "createdAt": "2024-01-15T10:30:00",
                                "updatedAt": "2024-01-15T10:35:00"
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Draft not found",
                content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden - User does not own this draft",
                content = @Content(mediaType = "application/json")
            )
        }
    )
    public ApiResponse<DraftListingResponse> updateDraft(
            @PathVariable Long draftId,
            @RequestBody DraftListingRequest request) {
        String userId = extractUserId();
        DraftListingResponse response = listingService.updateDraft(draftId, request, userId);
        return ApiResponse.<DraftListingResponse>builder().data(response).build();
    }

    /**
     * Get draft by ID
     * GET /v1/listings/draft/{draftId}
     */
    @GetMapping("/draft/{draftId}")
    @Operation(
        summary = "Get draft listing by ID",
        description = """
            Get a specific draft listing by its ID.
            Only the owner can view their draft.

            **Use case**: Load draft data when user wants to continue editing
            """,
        parameters = {
            @Parameter(name = "draftId", description = "Draft ID", required = true, example = "123")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved draft listing",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Draft Detail Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "draftId": 456,
                                "userId": "user-uuid-123",
                                "title": "Căn hộ 2 phòng ngủ ấm cúng",
                                "description": "Căn hộ rộng rãi với đầy đủ tiện nghi, view đẹp",
                                "listingType": "RENT",
                                "vipType": "NORMAL",
                                "categoryId": 10,
                                "productType": "APARTMENT",
                                "price": 5000000,
                                "priceUnit": "MONTH",
                                "address": {
                                  "addressType": "NEW",
                                  "newProvinceCode": "79",
                                  "newWardCode": "27001",
                                  "newStreet": "Nguyễn Văn Linh",
                                  "latitude": 10.7412,
                                  "longitude": 106.7220
                                },
                                "area": 75.5,
                                "bedrooms": 2,
                                "bathrooms": 2,
                                "direction": "SOUTH",
                                "furnishing": "FULLY_FURNISHED",
                                "roomCapacity": 4,
                                "waterPrice": "FREE",
                                "electricityPrice": "3500",
                                "internetPrice": "INCLUDED",
                                "serviceFee": "100000",
                                "amenities": [
                                  {
                                    "amenityId": 1,
                                    "name": "Wifi",
                                    "icon": "wifi",
                                    "description": "Free high-speed internet",
                                    "category": "BASIC",
                                    "isActive": true
                                  },
                                  {
                                    "amenityId": 2,
                                    "name": "Parking",
                                    "icon": "parking",
                                    "description": "Free parking space",
                                    "category": "BASIC",
                                    "isActive": true
                                  },
                                  {
                                    "amenityId": 3,
                                    "name": "Air Conditioning",
                                    "icon": "ac",
                                    "description": "Air conditioning in all rooms",
                                    "category": "BASIC",
                                    "isActive": true
                                  }
                                ],
                                "media": [
                                  {
                                    "mediaId": 201,
                                    "listingId": null,
                                    "userId": "user-uuid-123",
                                    "mediaType": "IMAGE",
                                    "sourceType": "UPLOAD",
                                    "status": "ACTIVE",
                                    "url": "https://pub-xxx.r2.dev/media/user-123/201-living-room.jpg",
                                    "thumbnailUrl": null,
                                    "title": "Living Room",
                                    "description": "Spacious living room",
                                    "altText": "Living room photo",
                                    "isPrimary": true,
                                    "sortOrder": 1,
                                    "fileSize": 2048576,
                                    "mimeType": "image/jpeg",
                                    "originalFilename": "living-room.jpg",
                                    "durationSeconds": null,
                                    "uploadConfirmed": true,
                                    "createdAt": "2024-01-15T10:25:00",
                                    "updatedAt": "2024-01-15T10:25:00"
                                  },
                                  {
                                    "mediaId": 202,
                                    "listingId": null,
                                    "userId": "user-uuid-123",
                                    "mediaType": "IMAGE",
                                    "sourceType": "UPLOAD",
                                    "status": "ACTIVE",
                                    "url": "https://pub-xxx.r2.dev/media/user-123/202-bedroom.jpg",
                                    "thumbnailUrl": null,
                                    "title": "Bedroom",
                                    "description": "Master bedroom",
                                    "altText": "Bedroom photo",
                                    "isPrimary": false,
                                    "sortOrder": 2,
                                    "fileSize": 1875432,
                                    "mimeType": "image/jpeg",
                                    "originalFilename": "bedroom.jpg",
                                    "durationSeconds": null,
                                    "uploadConfirmed": true,
                                    "createdAt": "2024-01-15T10:26:00",
                                    "updatedAt": "2024-01-15T10:26:00"
                                  }
                                ],
                                "createdAt": "2024-01-15T10:20:00",
                                "updatedAt": "2024-01-15T10:30:00"
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Draft not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found",
                        value = """
                            {
                              "code": "404001",
                              "message": "Draft not found",
                              "data": null
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden - User does not own this draft",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Forbidden",
                        value = """
                            {
                              "code": "403001",
                              "message": "You do not have permission to view this draft",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<DraftListingResponse> getDraftById(@PathVariable Long draftId) {
        String userId = extractUserId();
        DraftListingResponse response = listingService.getDraftById(draftId, userId);
        return ApiResponse.<DraftListingResponse>builder().data(response).build();
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

            **Response includes**:
            - Complete draft information with all fields
            - Full amenities array with complete amenity objects
            - Full media array with complete MediaResponse objects
            - All address and location details
            - Pricing and utility cost information
            - Property specifications (area, bedrooms, bathrooms, etc.)
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved draft listings",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = List.class),
                    examples = @ExampleObject(
                        name = "Draft Listings Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": [
                                {
                                  "draftId": 456,
                                  "userId": "user-uuid-123",
                                  "title": "Căn hộ 2 phòng ngủ ấm cúng tại Quận 7",
                                  "description": "Căn hộ 2PN rộng rãi, thoáng mát, full nội thất cao cấp. Gần trường học, siêu thị và công viên.",
                                  "listingType": "RENT",
                                  "propertyType": "APARTMENT",
                                  "area": 75.5,
                                  "price": 12000000,
                                  "priceUnit": "MONTH",
                                  "deposit": 24000000,
                                  "bedrooms": 2,
                                  "bathrooms": 2,
                                  "direction": "EAST",
                                  "furnishing": "FULL",
                                  "roomCapacity": 4,
                                  "numberOfFloors": 15,
                                  "availableFrom": "2024-02-01",
                                  "legalStatus": "RED_BOOK",
                                  "address": {
                                      "addressType": "NEW",
                                      "newProvinceCode": "79",
                                      "newWardCode": "27001",
                                      "newStreet": "Nguyễn Văn Linh",
                                      "latitude": 10.7412,
                                      "longitude": 106.7220
                                  },
                                  "houseNumber": "123",
                                  "latitude": 10.732157,
                                  "longitude": 106.719838,
                                  "waterPrice": 15000,
                                  "waterPriceUnit": "CUBIC_METER",
                                  "electricityPrice": 3500,
                                  "electricityPriceUnit": "KWH",
                                  "internetPrice": 200000,
                                  "internetPriceUnit": "MONTH",
                                  "serviceFee": 500000,
                                  "serviceFeeUnit": "MONTH",
                                  "allowPets": true,
                                  "videoLink": "https://www.youtube.com/watch?v=example123",
                                  "amenities": [
                                    {
                                      "amenityId": 1,
                                      "amenityName": "Điều hòa",
                                      "amenityNameEn": "Air Conditioning",
                                      "category": "CONVENIENCE",
                                      "icon": "ac_unit"
                                    },
                                    {
                                      "amenityId": 5,
                                      "amenityName": "Hồ bơi",
                                      "amenityNameEn": "Swimming Pool",
                                      "category": "FACILITY",
                                      "icon": "pool"
                                    },
                                    {
                                      "amenityId": 12,
                                      "amenityName": "An ninh 24/7",
                                      "amenityNameEn": "24/7 Security",
                                      "category": "SECURITY",
                                      "icon": "security"
                                    }
                                  ],
                                  "media": [
                                    {
                                      "mediaId": 201,
                                      "listingId": null,
                                      "userId": "user-uuid-123",
                                      "mediaType": "IMAGE",
                                      "sourceType": "UPLOAD",
                                      "url": "https://pub-xxx.r2.dev/media/user-123/201-living-room.jpg",
                                      "fileSize": 2048576,
                                      "mimeType": "image/jpeg",
                                      "width": 1920,
                                      "height": 1080,
                                      "displayOrder": 1,
                                      "caption": "Phòng khách rộng rãi",
                                      "thumbnailUrl": "https://pub-xxx.r2.dev/media/user-123/thumb_201-living-room.jpg",
                                      "uploadedAt": "2024-01-15T09:00:00",
                                      "metadata": null,
                                      "processingStatus": "COMPLETED",
                                      "externalVideoId": null,
                                      "externalPlatform": null,
                                      "embedUrl": null,
                                      "videoDuration": null,
                                      "videoTitle": null,
                                      "videoDescription": null,
                                      "originalFileName": "living-room-photo.jpg",
                                      "storagePath": "user-123/201-living-room.jpg",
                                      "cdnUrl": "https://cdn.smartrent.com/media/201-living-room.jpg"
                                    },
                                    {
                                      "mediaId": 202,
                                      "listingId": null,
                                      "userId": "user-uuid-123",
                                      "mediaType": "IMAGE",
                                      "sourceType": "UPLOAD",
                                      "url": "https://pub-xxx.r2.dev/media/user-123/202-bedroom.jpg",
                                      "fileSize": 1876432,
                                      "mimeType": "image/jpeg",
                                      "width": 1920,
                                      "height": 1080,
                                      "displayOrder": 2,
                                      "caption": "Phòng ngủ master",
                                      "thumbnailUrl": "https://pub-xxx.r2.dev/media/user-123/thumb_202-bedroom.jpg",
                                      "uploadedAt": "2024-01-15T09:05:00",
                                      "metadata": null,
                                      "processingStatus": "COMPLETED",
                                      "externalVideoId": null,
                                      "externalPlatform": null,
                                      "embedUrl": null,
                                      "videoDuration": null,
                                      "videoTitle": null,
                                      "videoDescription": null,
                                      "originalFileName": "bedroom-photo.jpg",
                                      "storagePath": "user-123/202-bedroom.jpg",
                                      "cdnUrl": "https://cdn.smartrent.com/media/202-bedroom.jpg"
                                    },
                                    {
                                      "mediaId": 203,
                                      "listingId": null,
                                      "userId": "user-uuid-123",
                                      "mediaType": "VIDEO",
                                      "sourceType": "YOUTUBE",
                                      "url": "https://www.youtube.com/watch?v=example123",
                                      "fileSize": null,
                                      "mimeType": null,
                                      "width": null,
                                      "height": null,
                                      "displayOrder": 3,
                                      "caption": "Video tour căn hộ",
                                      "thumbnailUrl": "https://i.ytimg.com/vi/example123/maxresdefault.jpg",
                                      "uploadedAt": "2024-01-15T09:10:00",
                                      "metadata": null,
                                      "processingStatus": "COMPLETED",
                                      "externalVideoId": "example123",
                                      "externalPlatform": "YOUTUBE",
                                      "embedUrl": "https://www.youtube.com/embed/example123",
                                      "videoDuration": 180,
                                      "videoTitle": "Căn hộ 2PN Quận 7 - Full nội thất",
                                      "videoDescription": "Video giới thiệu chi tiết căn hộ",
                                      "originalFileName": null,
                                      "storagePath": null,
                                      "cdnUrl": null
                                    }
                                  ],
                                  "createdAt": "2024-01-15T10:30:00",
                                  "updatedAt": "2024-01-20T15:45:00"
                                },
                                {
                                  "draftId": 789,
                                  "userId": "user-uuid-123",
                                  "title": "Nhà phố 3 tầng mặt tiền đường lớn",
                                  "description": "Nhà phố mới xây, thiết kế hiện đại, phù hợp kinh doanh hoặc ở",
                                  "listingType": "SELL",
                                  "propertyType": "HOUSE",
                                  "area": 120.0,
                                  "price": 8500000000,
                                  "priceUnit": "TOTAL",
                                  "deposit": null,
                                  "bedrooms": 4,
                                  "bathrooms": 3,
                                  "direction": "SOUTH",
                                  "furnishing": "BASIC",
                                  "roomCapacity": null,
                                  "numberOfFloors": 3,
                                  "availableFrom": null,
                                  "legalStatus": "PINK_BOOK",
                                  "address": {
                                      "addressType": "NEW",
                                      "newProvinceCode": "79",
                                      "newWardCode": "27001",
                                      "newStreet": "Nguyễn Văn Linh",
                                      "latitude": 10.7412,
                                      "longitude": 106.7220
                                  },
                                  "houseNumber": null,
                                  "latitude": 10.768432,
                                  "longitude": 106.698854,
                                  "waterPrice": null,
                                  "waterPriceUnit": null,
                                  "electricityPrice": null,
                                  "electricityPriceUnit": null,
                                  "internetPrice": null,
                                  "internetPriceUnit": null,
                                  "serviceFee": null,
                                  "serviceFeeUnit": null,
                                  "allowPets": null,
                                  "videoLink": null,
                                  "amenities": [
                                    {
                                      "amenityId": 8,
                                      "amenityName": "Chỗ đậu xe",
                                      "amenityNameEn": "Parking",
                                      "category": "CONVENIENCE",
                                      "icon": "local_parking"
                                    }
                                  ],
                                  "media": [
                                    {
                                      "mediaId": 301,
                                      "listingId": null,
                                      "userId": "user-uuid-123",
                                      "mediaType": "IMAGE",
                                      "sourceType": "UPLOAD",
                                      "url": "https://pub-xxx.r2.dev/media/user-123/301-front-view.jpg",
                                      "fileSize": 3145728,
                                      "mimeType": "image/jpeg",
                                      "width": 2560,
                                      "height": 1440,
                                      "displayOrder": 1,
                                      "caption": "Mặt tiền nhà",
                                      "thumbnailUrl": "https://pub-xxx.r2.dev/media/user-123/thumb_301-front-view.jpg",
                                      "uploadedAt": "2024-01-18T14:20:00",
                                      "metadata": null,
                                      "processingStatus": "COMPLETED",
                                      "externalVideoId": null,
                                      "externalPlatform": null,
                                      "embedUrl": null,
                                      "videoDuration": null,
                                      "videoTitle": null,
                                      "videoDescription": null,
                                      "originalFileName": "house-front.jpg",
                                      "storagePath": "user-123/301-front-view.jpg",
                                      "cdnUrl": "https://cdn.smartrent.com/media/301-front-view.jpg"
                                    }
                                  ],
                                  "createdAt": "2024-01-18T14:00:00",
                                  "updatedAt": "2024-01-18T16:30:00"
                                }
                              ]
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<List<DraftListingResponse>> getMyDrafts() {
        String userId = extractUserId();
        List<DraftListingResponse> response = listingService.getMyDrafts(userId);
        return ApiResponse.<List<DraftListingResponse>>builder().data(response).build();
    }

    /**
     * Publish draft listing
     * POST /v1/listings/draft/{draftId}/publish
     */
    @PostMapping("/draft/{draftId}/publish")
    @Operation(
        summary = "Publish draft listing",
        description = """
            Publish a draft listing after validating all required fields.
            The draft data is merged with the request body (request takes precedence).

            **Validation**:
            - Validates all required fields (title, description, price, address, etc.)
            - Creates a new listing in the listings table
            - Deletes the draft after successful publish

            **Required fields** (from draft or request):
            - title, description
            - listingType, productType
            - price, priceUnit
            - address
            - categoryId

            **Payment/Quota options** (in request body):
            - useMembershipQuota: true/false
            - benefitIds: [1, 2] (when using quota)
            - durationDays: 10/15/30 (when paying)
            - paymentProvider: VNPAY (when paying)

            **Use case**: User finishes editing draft and wants to publish
            """,
        parameters = {
            @Parameter(name = "draftId", description = "Draft ID", required = true, example = "123")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingCreationRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Publish with Quota",
                        summary = "Publish using membership quota",
                        value = """
                            {
                              "useMembershipQuota": true,
                              "benefitIds": [1]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Publish with Payment",
                        summary = "Publish with direct payment",
                        value = """
                            {
                              "vipType": "GOLD",
                              "durationDays": 30,
                              "paymentProvider": "VNPAY"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Publish with Override",
                        summary = "Publish with some fields overridden",
                        value = """
                            {
                              "title": "Updated Title",
                              "price": 15000000,
                              "useMembershipQuota": true,
                              "benefitIds": [1]
                            }
                            """
                    )
                }
            )
        ),
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
                description = "Missing required fields",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Validation Error",
                        value = """
                            {
                              "code": "400001",
                              "message": "Cannot publish draft. Missing required fields: title, description, price",
                              "data": null
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<ListingCreationResponse> publishDraft(
            @PathVariable Long draftId,
            @RequestBody ListingCreationRequest request) {
        String userId = extractUserId();
        ListingCreationResponse response = listingService.publishDraft(draftId, request, userId);
        return ApiResponse.<ListingCreationResponse>builder().data(response).build();
    }

    /**
     * Delete draft listing
     * DELETE /v1/listings/draft/{draftId}
     */
    @DeleteMapping("/draft/{draftId}")
    @Operation(
        summary = "Delete draft listing",
        description = """
            Delete a draft listing.

            **Use case**: User wants to discard a draft
            """,
        parameters = {
            @Parameter(name = "draftId", description = "Draft ID", required = true, example = "123")
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
                responseCode = "404",
                description = "Draft not found"
            )
        }
    )
    public ApiResponse<Void> deleteDraft(@PathVariable Long draftId) {
        String userId = extractUserId();
        listingService.deleteDraft(draftId, userId);
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