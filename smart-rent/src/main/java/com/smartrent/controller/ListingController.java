package com.smartrent.controller;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.service.listing.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/listings")
@Tag(name = "Listings", description = "CRUD operations for property listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @Operation(
        summary = "Create a new listing",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingCreationRequest.class),
                examples = @ExampleObject(
                    name = "Create Listing Example",
                    value = """
                        {
                          "title": "Căn hộ 2PN thoáng mát quận 1",
                          "description": "Căn hộ 2 phòng ngủ rộng rãi, có ban công và tầm nhìn đẹp ra thành phố. Gần chợ Bến Thành, tiện ích đầy đủ.",
                          "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                          "expiryDate": "2025-12-31T23:59:59",
                          "listingType": "RENT",
                          "verified": false,
                          "isVerify": false,
                          "expired": false,
                          "vipType": "NORMAL",
                          "categoryId": 10,
                          "productType": "APARTMENT",
                          "price": 15000000.00,
                          "priceUnit": "MONTH",
                          "addressId": 501,
                          "area": 78.5,
                          "bedrooms": 2,
                          "bathrooms": 1,
                          "direction": "NORTHEAST",
                          "furnishing": "SEMI_FURNISHED",
                          "propertyType": "APARTMENT",
                          "roomCapacity": 4,
                          "amenityIds": [1, 3, 5]
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing created",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Success Response",
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
                    )
                )
            )
        }
    )
    @PostMapping
    public ApiResponse<ListingCreationResponse> createListing(@Valid @RequestBody ListingCreationRequest request) {
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
                                "title": "Căn hộ 2PN thoáng mát quận 1",
                                "description": "Căn hộ 2 phòng ngủ rộng rãi, có ban công và tầm nhìn đẹp ra thành phố. Gần chợ Bến Thành, tiện ích đầy đủ.",
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
                                "price": 15000000.00,
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
                                    "name": "WiFi",
                                    "icon": "wifi",
                                    "description": "Internet không dây tốc độ cao",
                                    "category": "connectivity",
                                    "isActive": true
                                  },
                                  {
                                    "amenityId": 3,
                                    "name": "Chỗ đậu xe",
                                    "icon": "car",
                                    "description": "Chỗ đậu xe riêng biệt",
                                    "category": "parking",
                                    "isActive": true
                                  },
                                  {
                                    "amenityId": 5,
                                    "name": "Phòng gym",
                                    "icon": "dumbbell",
                                    "description": "Phòng tập gym 24/7",
                                    "category": "fitness",
                                    "isActive": true
                                  }
                                ],
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

    @GetMapping
    @Operation(
        summary = "List listings with detailed information",
        description = "When 'ids' is provided, returns those specific listings. Otherwise returns a paginated list using 'page' and 'size'. Returns full listing details including amenities, address, and timestamps.",
        parameters = {
            @Parameter(name = "ids", description = "Optional list of listing IDs to fetch explicitly"),
            @Parameter(name = "page", description = "Zero-based page index", example = "0"),
            @Parameter(name = "size", description = "Page size (max 100)", example = "20")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "List of detailed listings",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Detailed Listings Example",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": [
                                {
                                  "listingId": 123,
                                  "title": "Căn hộ 2PN thoáng mát quận 1",
                                  "description": "Căn hộ 2 phòng ngủ rộng rãi, có ban công và tầm nhìn đẹp ra thành phố. Gần chợ Bến Thành, tiện ích đầy đủ.",
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
                                  "price": 15000000.00,
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
                                      "name": "WiFi",
                                      "icon": "wifi",
                                      "description": "Internet không dây tốc độ cao",
                                      "category": "connectivity",
                                      "isActive": true
                                    },
                                    {
                                      "amenityId": 3,
                                      "name": "Chỗ đậu xe",
                                      "icon": "car",
                                      "description": "Chỗ đậu xe riêng biệt",
                                      "category": "parking",
                                      "isActive": true
                                    },
                                    {
                                      "amenityId": 5,
                                      "name": "Phòng gym",
                                      "icon": "dumbbell",
                                      "description": "Phòng tập gym 24/7",
                                      "category": "fitness",
                                      "isActive": true
                                    }
                                  ],
                                  "createdAt": "2025-09-01T10:00:00",
                                  "updatedAt": "2025-09-01T10:00:00"
                                },
                                {
                                  "listingId": 124,
                                  "title": "Studio hiện đại gần công viên Tao Đàn",
                                  "description": "Căn studio hiện đại với đầy đủ tiện nghi. Gần công viên Tao Đàn, yên tĩnh và thoáng mát.",
                                  "userId": "user-456e7890-e12b-34c5-d678-901234567890",
                                  "postDate": "2025-09-02T14:30:00",
                                  "expiryDate": "2025-12-31T23:59:59",
                                  "listingType": "RENT",
                                  "verified": true,
                                  "isVerify": true,
                                  "expired": false,
                                  "vipType": "VIP",
                                  "categoryId": 12,
                                  "productType": "STUDIO",
                                  "price": 8500000.00,
                                  "priceUnit": "MONTH",
                                  "addressId": 502,
                                  "area": 35.0,
                                  "bedrooms": 0,
                                  "bathrooms": 1,
                                  "direction": "SOUTH",
                                  "furnishing": "FULLY_FURNISHED",
                                  "propertyType": "STUDIO",
                                  "roomCapacity": 2,
                                  "amenities": [
                                    {
                                      "amenityId": 2,
                                      "name": "Điều hòa",
                                      "icon": "snowflake",
                                      "description": "Điều hòa trung tâm",
                                      "category": "climate",
                                      "isActive": true
                                    },
                                    {
                                      "amenityId": 4,
                                      "name": "Ban công",
                                      "icon": "home",
                                      "description": "Ban công riêng với tầm nhìn ra thành phố",
                                      "category": "outdoor",
                                      "isActive": true
                                    },
                                    {
                                      "amenityId": 6,
                                      "name": "Bể bơi",
                                      "icon": "swimmer",
                                      "description": "Bể bơi ngoài trời",
                                      "category": "recreation",
                                      "isActive": true
                                    }
                                  ],
                                  "createdAt": "2025-09-02T14:30:00",
                                  "updatedAt": "2025-09-02T14:30:00"
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
        @RequestParam(value = "page", defaultValue = "0") int page,
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
                          "title": "Căn hộ 2PN đã cải tạo mới quận 3",
                          "description": "Căn hộ đã được cải tạo lại với bếp mới và phòng tắm hiện đại. Vị trí thuận lợi gần trung tâm.",
                          "userId": 42,
                          "expiryDate": "2026-01-31T23:59:59",
                          "listingType": "RENT",
                          "vipType": "VIP",
                          "categoryId": 10,
                          "productType": "APARTMENT",
                          "price": 16500000.00,
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
}