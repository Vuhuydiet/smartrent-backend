package com.smartrent.controller;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Listings - CRUD",
    description = """
        Core property listing CRUD operations with VIP tiers, quota system, and transactional address creation.

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

            **Payment Flow:**
            1. Call GET /v1/vip-tiers/{tierCode} to get VIP tier information
            2. User selects vipType and durationDays from available options (10, 15, 30)
            3. Submit this request with durationDays
            4. Complete payment via returned paymentUrl
            5. Listing is created after successful payment

            **Required Fields for Standard Listing:**
            - title, description
            - listingType (RENT/SALE/SHARE)
            - vipType (NORMAL/SILVER/GOLD/DIAMOND)
            - categoryId
            - productType (APARTMENT/HOUSE/STUDIO/ROOM/OFFICE)
            - price, priceUnit (MONTH/DAY/YEAR)
            - address object with nested legacy or newAddress format
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
                              "useMembershipQuota": false,
                              "paymentProvider": "VNPAY"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Listing with Membership Quota",
                        summary = "Create VIP listing using membership quota",
                        value = """
                            {
                              "title": "Căn hộ cao cấp 3 phòng ngủ",
                              "description": "Căn hộ cao cấp với đầy đủ tiện nghi, view đẹp.",
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
                              "useMembershipQuota": true,
                              "benefitIds": [101, 102]
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
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation error - missing required fields for non-draft listing"
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
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
        }
    )
    @GetMapping("/{id}")
    public ApiResponse<ListingResponse> getListingById(@PathVariable Long id) {
        ListingResponse response = listingService.getListingById(id);
        return ApiResponse.<ListingResponse>builder().data(response).build();
    }

    @GetMapping
    @Operation(
        summary = "List listings",
        description = "When 'ids' is provided, returns those listings. Otherwise returns a paginated list using 'page' and 'size'.",
        parameters = {
            @Parameter(name = "ids", description = "Optional list of listing IDs to fetch explicitly"),
            @Parameter(name = "page", description = "Page number (1-based indexing)", example = "1"),
            @Parameter(name = "size", description = "Number of items per page (max 100)", example = "20")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "List of listings",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    public ApiResponse<List<ListingResponse>> getListings(
        @RequestParam(value = "ids", required = false) Set<Long> ids,
        @RequestParam(value = "page", defaultValue = "1") int page,
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
                schema = @Schema(implementation = ListingRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
        }
    )
    @PutMapping("/{id}")
    public ApiResponse<ListingResponse> updateListing(@PathVariable Long id, @RequestBody ListingRequest request) {
        String userId = extractUserId();
        ListingResponse response = listingService.updateListing(id, request, userId);
        return ApiResponse.<ListingResponse>builder().data(response).build();
    }

    @Operation(
        summary = "Delete a listing",
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
        }
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteListing(@PathVariable Long id) {
        listingService.deleteListing(id);
        return ApiResponse.<Void>builder().build();
    }

    /**
     * @deprecated Use POST /v1/listings instead. This endpoint will be removed in a future version.
     */
    @Deprecated
    @PostMapping("/vip")
    @Operation(
        summary = "[DEPRECATED] Create VIP or Premium listing - Use POST /v1/listings instead",
        description = """
            ⚠️ **DEPRECATED**: This endpoint is deprecated. Please use `POST /v1/listings` instead.
            The main `/v1/listings` endpoint now supports all listing types with the same payment and quota functionality.
            """,
        deprecated = true,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VipListingCreationRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing created or payment URL returned")
        }
    )
    public ApiResponse<Object> createVipListing(@Valid @RequestBody VipListingCreationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        request.setUserId(userId);
        Object response = listingService.createVipListing(request);
        return ApiResponse.builder().data(response).build();
    }

    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return authentication.getName();
    }
}
