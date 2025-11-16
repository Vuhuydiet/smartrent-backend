package com.smartrent.controller;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.MyListingsFilterRequest;
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

            **Required Fields for Standard Listing:**
            - title, description
            - listingType (RENT/SALE/SHARE)
            - vipType (NORMAL/SILVER/GOLD/DIAMOND)
            - categoryId
            - productType (APARTMENT/HOUSE/STUDIO/ROOM/OFFICE)
            - price, priceUnit (MONTH/DAY/YEAR)
            - address object with:
              - addressType (OLD or NEW) - REQUIRED
              - For OLD: provinceId, districtId, wardId
              - For NEW: newProvinceCode, newWardCode

            **Optional Fields:**
            - isDraft (default: false)
            - area, bedrooms, bathrooms
            - direction, furnishing, propertyType
            - roomCapacity
            - amenityIds (array of amenity IDs)
            - mediaIds (array of uploaded media IDs)
            - address.streetId, address.streetNumber
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
                        name = "Standard Listing",
                        summary = "Create complete listing (published immediately)",
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
                                "addressType": "OLD",
                                "streetNumber": "123",
                                "streetId": 1,
                                "wardId": 1,
                                "districtId": 1,
                                "provinceId": 1,
                                "latitude": 21.0285,
                                "longitude": 105.8542
                              },
                              "area": 78.5,
                              "bedrooms": 2,
                              "bathrooms": 1,
                              "direction": "NORTHEAST",
                              "furnishing": "SEMI_FURNISHED",
                              "propertyType": "APARTMENT",
                              "roomCapacity": 4,
                              "amenityIds": [1, 3, 5],
                              "mediaIds": [101, 102, 103],
                              "isDraft": false
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
                        summary = "Create NORMAL listing with duration (requires payment)",
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
                                "addressType": "NEW",
                                "streetNumber": "45",
                                "newProvinceCode": "01",
                                "newWardCode": "00001",
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
                                "userId": "user-123e4567-e89b-12d3-a456-426614174000",
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
                                "addressId": 501,
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
        summary = "Search and filter listings with pagination and recommendations",
        description = """
            Search listings with comprehensive filtering options including:
            - **Category filtering**: Filter by category ID
            - **Province filtering**: Supports both old (63 provinces) and new (34 provinces) address structures
            - **Price range**: Min/max price filtering
            - **Area range**: Min/max area in square meters
            - **Listing type**: RENT, SALE, SHARE
            - **VIP type**: NORMAL, SILVER, GOLD, DIAMOND
            - **Product type**: ROOM, APARTMENT, HOUSE, OFFICE, STUDIO
            - **Bedrooms/Bathrooms**: Filter by number of rooms
            - **Verified status**: Show only verified listings
            - **Sorting**: Sort by postDate, price, area, createdAt (ASC/DESC)

            **Response includes:**
            - Paginated list of listings matching filter criteria
            - Total count of matching listings
            - Current page, page size, total pages
            - Recommendations: Top 5 high-tier (GOLD/DIAMOND) listings in same category
              (Placeholder for future ML-based recommendation system)

            **Notes:**
            - Draft listings are automatically excluded from search results
            - Shadow listings are excluded
            - Expired listings can be excluded with `excludeExpired: true` (default)
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingFilterRequest.class),
                examples = {
                    @ExampleObject(
                        name = "1. Public Search - By Location",
                        summary = "Find apartments in Hanoi, Ba Dinh District",
                        value = """
                            {
                              "categoryId": 1,
                              "provinceId": 1,
                              "districtId": 5,
                              "listingType": "RENT",
                              "productType": "APARTMENT",
                              "minPrice": 5000000,
                              "maxPrice": 15000000,
                              "verified": true,
                              "page": 0,
                              "size": 20,
                              "sortBy": "postDate",
                              "sortDirection": "DESC"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "2. My Draft Listings",
                        summary = "Get my draft listings (auto-saved)",
                        value = """
                            {
                              "userId": "user-123",
                              "isDraft": true,
                              "page": 0,
                              "size": 20,
                              "sortBy": "updatedAt",
                              "sortDirection": "DESC"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "3. My Active Listings",
                        summary = "Get my verified, non-expired listings",
                        value = """
                            {
                              "userId": "user-123",
                              "verified": true,
                              "expired": false,
                              "isDraft": false,
                              "page": 0,
                              "size": 20
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "4. Keyword Search",
                        summary = "Search by keyword in title and description",
                        value = """
                            {
                              "keyword": "căn hộ cao cấp view đẹp",
                              "provinceId": 1,
                              "verified": true,
                              "page": 0,
                              "size": 20
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "5. Filter by Amenities",
                        summary = "Find listings with specific amenities (AC, WiFi, Washing Machine)",
                        value = """
                            {
                              "provinceId": 1,
                              "amenityIds": [1, 3, 5],
                              "amenityMatchMode": "ALL",
                              "verified": true,
                              "page": 0,
                              "size": 20
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "6. Filter by Property Specs",
                        summary = "2-3 bedrooms, fully furnished, south facing",
                        value = """
                            {
                              "minBedrooms": 2,
                              "maxBedrooms": 3,
                              "furnishing": "FULLY_FURNISHED",
                              "direction": "SOUTH",
                              "minArea": 60.0,
                              "hasMedia": true,
                              "verified": true,
                              "page": 0,
                              "size": 20
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "7. Recent Listings",
                        summary = "Listings posted in last 7 days with photos",
                        value = """
                            {
                              "postedWithinDays": 7,
                              "hasMedia": true,
                              "verified": true,
                              "page": 0,
                              "size": 20,
                              "sortBy": "postDate",
                              "sortDirection": "DESC"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "8. Verified Owner Contacts",
                        summary = "Only listings with verified owner phone",
                        value = """
                            {
                              "ownerPhoneVerified": true,
                              "verified": true,
                              "provinceId": 1,
                              "page": 0,
                              "size": 20
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
                                    "userId": "user-abc",
                                    "price": 12000000,
                                    "priceUnit": "MONTH",
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

        // If userId is not provided in filter but user is authenticated, use authenticated user
        // This allows frontend to omit userId for "my listings" - backend auto-fills from JWT
        if ((filter.getUserId() == null || filter.getUserId().isEmpty()) &&
            authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getName())) {

            // Check if this is a "my listings" request by looking at isDraft or other ownership filters
            if (filter.getIsDraft() != null || filter.getIsVerify() != null) {
                filter.setUserId(authentication.getName());
            }
        }

        ListingListResponse response = listingService.searchListings(filter);
        return ApiResponse.<ListingListResponse>builder().data(response).build();
    }

    @Deprecated
    @PostMapping("/my-listings")
    @Operation(
        summary = "[DEPRECATED] Get current user's listings - Use /search instead",
        description = """
            **⚠️ DEPRECATED - Use POST /v1/listings/search instead**

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
                              "sortBy": "updatedAt",
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
                              "sortBy": "postDate",
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
                              "sortBy": "createdAt",
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
                              "sortBy": "updatedAt",
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
                              "sortBy": "createdAt",
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
                                  "userId": "user-123e4567-e89b-12d3-a456-426614174000",
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
                                  "addressId": 501,
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
                                  "userId": "user-456e7890-e89b-12d3-a456-426614174001",
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
                                  "addressId": 502,
                                  "area": 35.0,
                                  "bedrooms": 0,
                                  "bathrooms": 1,
                                  "direction": "SOUTH",
                                  "furnishing": "FULLY_FURNISHED",
                                  "propertyType": "APARTMENT",
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
                          "propertyType": "APARTMENT",
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
                        summary = "SILVER listing sử dụng quota từ membership",
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
                                "streetNumber": "45A",
                                "streetId": 10,
                                "wardId": 1,
                                "districtId": 1,
                                "provinceId": 1,
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
                        summary = "GOLD listing với thanh toán VNPay",
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
                                "streetNumber": "120",
                                "streetId": 50,
                                "wardId": 20,
                                "districtId": 10,
                                "provinceId": 79,
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
                        summary = "DIAMOND listing (tự động tạo shadow NORMAL listing)",
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
                                "streetNumber": "720A",
                                "streetId": 100,
                                "wardId": 50,
                                "districtId": 20,
                                "provinceId": 79,
                                "latitude": 10.7941,
                                "longitude": 106.7218
                              },
                              "area": 500.0,
                              "bedrooms": 5,
                              "bathrooms": 5,
                              "direction": "NORTHEAST",
                              "furnishing": "LUXURY_FURNISHED",
                              "propertyType": "PENTHOUSE",
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