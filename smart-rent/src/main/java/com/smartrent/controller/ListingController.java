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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
                          "title": "Cozy 2BR Apartment in Downtown",
                          "description": "Spacious 2-bedroom apartment with balcony and city view.",
                          "userId": "user-123e4567-e89b-12d3-a456-426614174000",
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
                                "title": "Cozy 2BR Apartment in Downtown",
                                "description": "Spacious 2-bedroom apartment with balcony and city view.",
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
                                "amenityIds": [1,3,5],
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
                                  "title": "Cozy 2BR Apartment in Downtown",
                                  "price": 1200.00,
                                  "priceUnit": "MONTH",
                                  "productType": "APARTMENT"
                                },
                                {
                                  "listingId": 124,
                                  "title": "Modern Studio near Park",
                                  "price": 700.00,
                                  "priceUnit": "MONTH",
                                  "productType": "STUDIO"
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

    // ----------------------------------------------------
    // Filter Listings Endpoint (GET /v1/listings/filter)
    // ----------------------------------------------------
    @Operation(
        summary = "Filter listings (GET)",
        description = "Dynamic AND-based filtering across numeric, boolean, enum và vị trí. Hỗ trợ cả ID và tên địa lý (contains, case-insensitive). Khi truyền cả ID và tên cùng cấp (ví dụ provinceId & provinceName) chúng sẽ được kết hợp AND (có thể trả về rỗng nếu không khớp). Tham số q tìm trong fullAddress. amenities có thể lặp nhiều lần: amenities=1&amenities=2.",
        parameters = {
            @Parameter(name = "addressId", description = "Filter theo addressId cụ thể (ưu tiên tuyệt đối nếu cung cấp)", example = "501"),
            @Parameter(name = "provinceId", description = "Province ID", example = "1"),
            @Parameter(name = "provinceName", description = "Tên tỉnh/thành (LIKE %value%) ví dụ: Hà Nội, TP.HCM", example = "Hà Nội"),
            @Parameter(name = "districtId", description = "District ID", example = "101"),
            @Parameter(name = "districtName", description = "Tên quận/huyện (LIKE %value%)", example = "Quận 1"),
            @Parameter(name = "wardName", description = "Tên phường/xã (LIKE %value%)", example = "Phường Bến Nghé"),
            @Parameter(name = "streetId", description = "Street ID", example = "999"),
            @Parameter(name = "streetName", description = "Tên đường/phố (LIKE %value%); không cần tiền tố 'Đường', 'Phố'", example = "Lê Lợi"),
            @Parameter(name = "q", description = "Full text trên fullAddress (LIKE %value%)", example = "123 Lê Lợi Quận 1"),
            @Parameter(name = "categoryId", description = "Category ID", example = "9"),
            @Parameter(name = "priceMin", description = "Giá tối thiểu (>=)", example = "3000000"),
            @Parameter(name = "priceMax", description = "Giá tối đa (<=)", example = "7000000"),
            @Parameter(name = "areaMin", description = "Diện tích tối thiểu (m2)", example = "15"),
            @Parameter(name = "areaMax", description = "Diện tích tối đa (m2)", example = "40"),
            @Parameter(name = "amenities", description = "Danh sách amenityId (lặp param): amenities=1&amenities=2", example = "1"),
            @Parameter(name = "status", description = "Trạng thái listing (nếu hệ thống dùng field status)", example = "ACTIVE"),
            @Parameter(name = "verified", description = "Đã xác minh hay chưa", example = "true"),
            @Parameter(name = "bedrooms", description = "Số phòng ngủ đúng bằng", example = "2"),
            @Parameter(name = "direction", description = "Hướng (enum Listing.Direction)", example = "NORTH"),
            @Parameter(name = "page", description = "Trang (0-based)", example = "0"),
            @Parameter(name = "size", description = "Kích thước trang (1-100)", example = "20")
        }
    )
    @GetMapping("/filter")
    public ApiResponse<com.smartrent.dto.response.ListingFilterResponse> filterListings(
        @RequestParam(required = false) Long addressId,
        @RequestParam(required = false) Long provinceId,
        @RequestParam(required = false) Long districtId,
        @RequestParam(required = false) Long streetId,
        @RequestParam(required = false) String provinceName,
        @RequestParam(required = false) String districtName,
        @RequestParam(required = false) String wardName,
        @RequestParam(required = false) String streetName,
        @RequestParam(required = false, name = "q") String addressText,
    @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long priceMin,
        @RequestParam(required = false) Long priceMax,
        @RequestParam(required = false) Integer areaMin,
        @RequestParam(required = false) Integer areaMax,
        @RequestParam(required = false) List<Long> amenities,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Boolean verified,
        @RequestParam(required = false) Integer bedrooms,
        @RequestParam(required = false) String direction,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size
    ) {
        com.smartrent.dto.request.ListingFilterRequest filterRequest = com.smartrent.dto.request.ListingFilterRequest.builder()
            .addressId(addressId)
            .provinceId(provinceId)
            .districtId(districtId)
            .streetId(streetId)
            .provinceName(provinceName)
            .districtName(districtName)
            .wardName(wardName)
            .streetName(streetName)
            .addressText(addressText)
            .categoryId(categoryId)
            .priceMin(priceMin)
            .priceMax(priceMax)
            .areaMin(areaMin)
            .areaMax(areaMax)
            .amenities(amenities)
            .status(status)
            .verified(verified)
            .bedrooms(bedrooms)
            .direction(direction)
            .page(page)
            .size(size)
            .build();
        var resp = listingService.filterListings(filterRequest);
        return ApiResponse.<com.smartrent.dto.response.ListingFilterResponse>builder()
            .code("999999")
            .data(resp)
            .build();
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
                          "title": "Updated 2BR Apartment with New Photos",
                          "description": "Now includes a renovated kitchen and updated bathroom.",
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
}