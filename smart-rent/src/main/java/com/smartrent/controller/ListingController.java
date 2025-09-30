package com.smartrent.controller;

import com.smartrent.controller.dto.request.ListingCreationRequest;
import com.smartrent.controller.dto.request.ListingRequest;
import com.smartrent.controller.dto.response.ListingCreationResponse;
import com.smartrent.controller.dto.response.ListingResponse;
import com.smartrent.controller.dto.request.ListingFilterRequest;
import com.smartrent.controller.dto.response.ListingFilterResponse;
import com.smartrent.controller.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.smartrent.service.listing.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            content = @io.swagger.v3.oas.annotations.media.Content(
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ListingCreationRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing created",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ListingCreationResponse.class)
                )
            )
        }
    )
    @PostMapping
    public ResponseEntity<ListingCreationResponse> createListing(@Valid @RequestBody ListingCreationRequest request) {
        ListingCreationResponse response = listingService.createListing(request);
        return ResponseEntity.ok(response);
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
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ListingResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found"
            )
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        ListingResponse response = listingService.getListingById(id);
        return ResponseEntity.ok(response);
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
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ListingResponse.class)
                )
            )
        }
    )
    public ResponseEntity<List<ListingResponse>> getListings(
        @RequestParam(value = "ids", required = false) Set<Long> ids,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        if (ids != null && !ids.isEmpty()) {
            List<ListingResponse> responses = listingService.getListingsByIds(ids);
            return ResponseEntity.ok(responses);
        }
        List<ListingResponse> responses = listingService.getListings(page, size);
        return ResponseEntity.ok(responses);
    }

        @Operation(
                summary = "Filter listings (GET)",
                description = """
Filter listings by multiple optional query parameters (AND logic). Authentication: not required.

Parameters (all optional): addressId, provinceId, districtId, streetId, category, priceMin/priceMax (VND), areaMin/areaMax (m²), amenities (repeatable), status, verified, bedrooms, direction, page, size.

Enum style fields (returned in response):
- listingType: RENT | SALE | SHARE
- vipType: NORMAL | VIP | PREMIUM
- productType: ROOM | APARTMENT | HOUSE | OFFICE | STUDIO
- direction: NORTH | SOUTH | EAST | WEST | NORTHEAST | NORTHWEST | SOUTHEAST | SOUTHWEST (when provided)
- priceUnit: MONTH | DAY | YEAR

Validation notes:
- If both priceMin & priceMax provided: priceMin ≤ priceMax.
- If both areaMin & areaMax provided: areaMin ≤ areaMax.
- amenities can repeat: amenities=1&amenities=2.

Examples:
    /v1/listings/filter
    /v1/listings/filter?priceMin=1000000&priceMax=5000000&districtId=1&page=0&size=20
    /v1/listings/filter?amenities=1&amenities=2&verified=true

Pagination is 0-based. Default page=0, size=20.
"""
        )
    @Parameters({
        @Parameter(name = "addressId", description = "Filter by exact address ID", required = false),
        @Parameter(name = "provinceId", description = "Filter by province ID", required = false),
        @Parameter(name = "districtId", description = "Filter by district ID", required = false),
        @Parameter(name = "streetId", description = "Filter by street ID", required = false),
        @Parameter(name = "category", description = "Category name or slug", required = false),
        @Parameter(name = "priceMin", description = "Minimum price (VND)", example = "1000000"),
        @Parameter(name = "priceMax", description = "Maximum price (VND)", example = "5000000"),
        @Parameter(name = "areaMin", description = "Minimum area (m²)", example = "10"),
        @Parameter(name = "areaMax", description = "Maximum area (m²)", example = "30"),
        @Parameter(name = "amenities", description = "Repeatable amenity IDs (amenities=1&amenities=2)", required = false),
        @Parameter(name = "status", description = "Listing status filter", required = false),
        @Parameter(name = "verified", description = "Only verified listings if true", required = false),
        @Parameter(name = "bedrooms", description = "Exact number of bedrooms", example = "2"),
        @Parameter(name = "direction", description = "Property direction (e.g., NORTH, SOUTH, EAST, WEST, NORTHEAST)", required = false),
        @Parameter(name = "page", description = "Page index (0-based)", example = "0"),
        @Parameter(name = "size", description = "Page size (default 20)", example = "20")
    })
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<ListingFilterResponse>> filterListings(
        @RequestParam(required = false) Long addressId,
        @RequestParam(required = false) Long provinceId,
        @RequestParam(required = false) Long districtId,
        @RequestParam(required = false) Long streetId,
        @RequestParam(required = false) String category,
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
        ListingFilterRequest filterRequest = ListingFilterRequest.builder()
            .addressId(addressId)
            .provinceId(provinceId)
            .districtId(districtId)
            .streetId(streetId)
            .category(category)
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
        ListingFilterResponse resp = listingService.filterListings(filterRequest);
        ApiResponse<ListingFilterResponse> apiResp = ApiResponse.<ListingFilterResponse>builder()
            .code("200")
            .message("Filter listings successfully")
            .data(resp)
            .build();
        return ResponseEntity.ok(apiResp);
    }

    @Operation(
        summary = "Update a listing",
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ListingRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing updated",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ListingResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found"
            )
        }
    )
    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> updateListing(@PathVariable Long id, @RequestBody ListingRequest request) {
        ListingResponse response = listingService.updateListing(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Delete a listing",
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "Listing deleted"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found"
            )
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        listingService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }
}