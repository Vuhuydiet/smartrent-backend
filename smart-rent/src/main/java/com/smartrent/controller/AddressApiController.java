package com.smartrent.controller;

import com.smartrent.dto.request.BatchAddressConversionRequest;
import com.smartrent.dto.response.*;
import com.smartrent.service.address.EnhancedAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Enhanced Address API Controller
 * Provides comprehensive address management with code-based operations,
 * administrative reorganization support, and conversion features
 *
 * @author Senior Java Spring Boot Engineer
 * @version 2.0
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Enhanced Address API", description = "Comprehensive Vietnamese administrative address API with merge/conversion support")
@RequiredArgsConstructor
@Slf4j
public class AddressApiController {

    private final EnhancedAddressService enhancedAddressService;

    // ========================================================================
    // Current/Active Administrative Structure Endpoints
    // ========================================================================

    @GetMapping("/provinces")
    @Operation(
            summary = "Get all provinces",
            description = "Returns all active provinces in the current administrative structure (excludes merged provinces)",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "List of active provinces",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ProvinceResponse.class))
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getAllProvinces() {
        log.info("GET /api/v1/provinces - Fetching all active provinces");

        List<ProvinceResponse> provinces = enhancedAddressService.getAllProvinces();

        return ResponseEntity.ok(ApiResponse.<List<ProvinceResponse>>builder()
                .data(provinces)
                .message("Successfully retrieved " + provinces.size() + " provinces")
                .build());
    }

    @GetMapping("/provinces/{provinceCode}/districts")
    @Operation(
            summary = "Get districts by province code",
            description = "Returns all active districts for a given province code",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "List of districts",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = DistrictResponse.class))
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Province not found"
                    )
            }
    )
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistrictsByProvinceCode(
            @Parameter(description = "Province code (e.g., '01' for Hanoi)", example = "01")
            @PathVariable String provinceCode) {

        log.info("GET /api/v1/provinces/{}/districts", provinceCode);

        List<DistrictResponse> districts = enhancedAddressService.getDistrictsByProvinceCode(provinceCode);

        return ResponseEntity.ok(ApiResponse.<List<DistrictResponse>>builder()
                .data(districts)
                .message("Successfully retrieved " + districts.size() + " districts")
                .build());
    }

    @GetMapping("/districts/{districtCode}/wards")
    @Operation(
            summary = "Get wards by district code",
            description = "Returns all active wards for a given district code",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "List of wards",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = WardResponse.class))
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "District not found"
                    )
            }
    )
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWardsByDistrictCode(
            @Parameter(description = "District code (e.g., '001' for Ba Dinh)", example = "001")
            @PathVariable String districtCode) {

        log.info("GET /api/v1/districts/{}/wards", districtCode);

        List<WardResponse> wards = enhancedAddressService.getWardsByDistrictCode(districtCode);

        return ResponseEntity.ok(ApiResponse.<List<WardResponse>>builder()
                .data(wards)
                .message("Successfully retrieved " + wards.size() + " wards")
                .build());
    }

    @GetMapping("/full-address")
    @Operation(
            summary = "Build full address from codes",
            description = "Constructs complete address hierarchy from province, district, and ward codes",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Full address details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FullAddressResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "One or more address components not found"
                    )
            }
    )
    public ResponseEntity<ApiResponse<FullAddressResponse>> getFullAddress(
            @Parameter(description = "Province code", example = "01", required = true)
            @RequestParam String provinceCode,

            @Parameter(description = "District code", example = "001", required = true)
            @RequestParam String districtCode,

            @Parameter(description = "Ward code", example = "00001", required = true)
            @RequestParam String wardCode) {

        log.info("GET /api/v1/full-address - provinceCode={}, districtCode={}, wardCode={}",
                provinceCode, districtCode, wardCode);

        FullAddressResponse fullAddress = enhancedAddressService.getFullAddress(provinceCode, districtCode, wardCode);

        return ResponseEntity.ok(ApiResponse.<FullAddressResponse>builder()
                .data(fullAddress)
                .message("Successfully built full address")
                .build());
    }

    // ========================================================================
    // New Administrative Structure Endpoints (includes merged entities)
    // ========================================================================

    @GetMapping("/new-provinces")
    @Operation(
            summary = "Get all provinces (new structure)",
            description = "Returns all provinces including merged ones with parent references. " +
                    "Useful for historical data and conversion purposes.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "List of all provinces including merged entities",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ProvinceResponse.class))
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getNewProvinces() {
        log.info("GET /api/v1/new-provinces - Fetching all provinces (including merged)");

        List<ProvinceResponse> provinces = enhancedAddressService.getNewProvinces();

        return ResponseEntity.ok(ApiResponse.<List<ProvinceResponse>>builder()
                .data(provinces)
                .message("Successfully retrieved " + provinces.size() + " provinces (including merged)")
                .build());
    }

    @GetMapping("/new-provinces/{provinceCode}/wards")
    @Operation(
            summary = "Get wards directly by province (flattened)",
            description = "Returns all wards under a province (bypassing district hierarchy). " +
                    "Useful for simplified province-ward selection.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "List of wards",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = WardResponse.class))
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWardsByProvinceCode(
            @Parameter(description = "Province code", example = "01")
            @PathVariable String provinceCode) {

        log.info("GET /api/v1/new-provinces/{}/wards", provinceCode);

        List<WardResponse> wards = enhancedAddressService.getWardsByProvinceCode(provinceCode);

        return ResponseEntity.ok(ApiResponse.<List<WardResponse>>builder()
                .data(wards)
                .message("Successfully retrieved " + wards.size() + " wards")
                .build());
    }

    @GetMapping("/new-full-address")
    @Operation(
            summary = "Build full address (new structure)",
            description = "Constructs full address considering merged entities and administrative changes",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Full address details with merge information",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FullAddressResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<FullAddressResponse>> getNewFullAddress(
            @Parameter(description = "Province code (can be old/merged code)", example = "01")
            @RequestParam String provinceCode,

            @Parameter(description = "District code", example = "001")
            @RequestParam String districtCode,

            @Parameter(description = "Ward code (can be old/merged code)", example = "00001")
            @RequestParam String wardCode) {

        log.info("GET /api/v1/new-full-address - provinceCode={}, districtCode={}, wardCode={}",
                provinceCode, districtCode, wardCode);

        FullAddressResponse fullAddress = enhancedAddressService.getNewFullAddress(provinceCode, districtCode, wardCode);

        return ResponseEntity.ok(ApiResponse.<FullAddressResponse>builder()
                .data(fullAddress)
                .message("Successfully built full address with merge handling")
                .build());
    }

    // ========================================================================
    // Search Endpoints
    // ========================================================================

    @GetMapping("/search-address")
    @Operation(
            summary = "Search addresses (current structure)",
            description = "Search for addresses using current/active administrative structure. " +
                    "Returns matching provinces, districts, and wards with relevance scores.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Search results",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AddressSearchResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<AddressSearchResponse>> searchAddress(
            @Parameter(description = "Search query (province, district, or ward name)", example = "Ba Đình")
            @RequestParam String q,

            @Parameter(description = "Maximum number of results", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {

        log.info("GET /api/v1/search-address - query={}, limit={}", q, limit);

        AddressSearchResponse searchResults = enhancedAddressService.searchAddress(q, limit);

        return ResponseEntity.ok(ApiResponse.<AddressSearchResponse>builder()
                .data(searchResults)
                .message("Found " + searchResults.getTotalResults() + " matches")
                .build());
    }

    @GetMapping("/search-new-address")
    @Operation(
            summary = "Search addresses (new structure)",
            description = "Search for addresses including merged/historical entities. " +
                    "Useful for converting old addresses to new structure.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Search results including merged entities",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AddressSearchResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<AddressSearchResponse>> searchNewAddress(
            @Parameter(description = "Search query", example = "Hà Tây")
            @RequestParam String q,

            @Parameter(description = "Maximum number of results", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {

        log.info("GET /api/v1/search-new-address - query={}, limit={}", q, limit);

        AddressSearchResponse searchResults = enhancedAddressService.searchNewAddress(q, limit);

        return ResponseEntity.ok(ApiResponse.<AddressSearchResponse>builder()
                .data(searchResults)
                .message("Found " + searchResults.getTotalResults() + " matches (including merged)")
                .build());
    }

    // ========================================================================
    // Address Conversion Endpoints
    // ========================================================================

    @GetMapping("/convert/address")
    @Operation(
            summary = "Convert single address",
            description = "Convert address from old administrative structure to new structure. " +
                    "Handles province mergers (e.g., Hà Tây → Hà Nội) and ward reorganizations.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Address conversion result",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AddressConversionResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Address not found"
                    )
            }
    )
    public ResponseEntity<ApiResponse<AddressConversionResponse>> convertAddress(
            @Parameter(description = "Old province code", example = "35")
            @RequestParam String provinceCode,

            @Parameter(description = "Old district code", example = "001")
            @RequestParam String districtCode,

            @Parameter(description = "Old ward code", example = "00001")
            @RequestParam String wardCode) {

        log.info("GET /api/v1/convert/address - provinceCode={}, districtCode={}, wardCode={}",
                provinceCode, districtCode, wardCode);

        AddressConversionResponse conversion = enhancedAddressService.convertAddress(provinceCode, districtCode, wardCode);

        return ResponseEntity.ok(ApiResponse.<AddressConversionResponse>builder()
                .data(conversion)
                .message(conversion.getConversionInfo().getWasConverted()
                        ? "Address converted successfully"
                        : "No conversion needed")
                .build());
    }

    @PostMapping("/convert/batch")
    @Operation(
            summary = "Batch convert addresses",
            description = "Convert multiple addresses at once. Useful for bulk data migration. " +
                    "Maximum 1000 addresses per request.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Batch conversion results",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AddressConversionResponse.class))
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid request (empty list or exceeds 1000 addresses)"
                    )
            }
    )
    public ResponseEntity<ApiResponse<List<AddressConversionResponse>>> batchConvertAddresses(
            @Valid @RequestBody BatchAddressConversionRequest request) {

        log.info("POST /api/v1/convert/batch - Converting {} addresses", request.getAddresses().size());

        List<AddressConversionResponse> results = enhancedAddressService.convertAddressesBatch(request);

        long convertedCount = results.stream()
                .filter(r -> r.getConversionInfo().getWasConverted())
                .count();

        return ResponseEntity.ok(ApiResponse.<List<AddressConversionResponse>>builder()
                .data(results)
                .message(String.format("Converted %d out of %d addresses", convertedCount, results.size()))
                .build());
    }

    // ========================================================================
    // Merge History Endpoints
    // ========================================================================

    @GetMapping("/merge-history/province/{code}")
    @Operation(
            summary = "Get province merge history",
            description = "Returns merge history for a province. Shows if it was merged into another province " +
                    "or if it's a parent province that absorbed others.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Province merge history",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = MergeHistoryResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Province not found"
                    )
            }
    )
    public ResponseEntity<ApiResponse<MergeHistoryResponse>> getProvinceMergeHistory(
            @Parameter(description = "Province code", example = "35")
            @PathVariable String code) {

        log.info("GET /api/v1/merge-history/province/{}", code);

        MergeHistoryResponse history = enhancedAddressService.getProvinceMergeHistory(code);

        return ResponseEntity.ok(ApiResponse.<MergeHistoryResponse>builder()
                .data(history)
                .message("Successfully retrieved merge history")
                .build());
    }

    @GetMapping("/merge-history/ward/{code}")
    @Operation(
            summary = "Get ward merge history",
            description = "Returns merge history for a ward. Shows reorganization details.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Ward merge history",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = MergeHistoryResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Ward not found"
                    )
            }
    )
    public ResponseEntity<ApiResponse<MergeHistoryResponse>> getWardMergeHistory(
            @Parameter(description = "Ward code", example = "00001")
            @PathVariable String code) {

        log.info("GET /api/v1/merge-history/ward/{}", code);

        MergeHistoryResponse history = enhancedAddressService.getWardMergeHistory(code);

        return ResponseEntity.ok(ApiResponse.<MergeHistoryResponse>builder()
                .data(history)
                .message("Successfully retrieved merge history")
                .build());
    }

    // ========================================================================
    // Health Check & Utility Endpoints
    // ========================================================================

    @GetMapping("/health")
    @Operation(
            summary = "API health check",
            description = "Simple health check endpoint",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "API is healthy"
                    )
            }
    )
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .data("OK")
                .message("Address API is healthy")
                .build());
    }
}
