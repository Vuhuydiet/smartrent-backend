package com.smartrent.controller;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.dto.response.*;
import com.smartrent.service.address.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/addresses")
@Tag(name = "Addresses", description = "Administrative address endpoints for provinces, districts, wards, streets, and addresses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressController {

    AddressService addressService;

    // Province endpoints
    @GetMapping("/provinces")
    @Operation(
        summary = "List provinces",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Provinces retrieved",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProvinceResponse.class)),
                    examples = @ExampleObject(
                        name = "Provinces Example",
                        value = """
                            [ { "provinceId": 1, "name": "Hanoi" }, { "provinceId": 2, "name": "Ho Chi Minh City" } ]
                            """
                    )
                )
            )
        }
    )
    ApiResponse<List<ProvinceResponse>> getAllProvinces() {
        List<ProvinceResponse> provinces = addressService.getParentProvinces();
        return ApiResponse.<List<ProvinceResponse>>builder()
                .data(provinces)
                .build();
    }

    @GetMapping("/provinces/{provinceId}")
    @Operation(
        summary = "Get province by ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Province retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProvinceResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Province not found"
            )
        }
    )
    ApiResponse<ProvinceResponse> getProvinceById(@PathVariable Long provinceId) {
        ProvinceResponse province = addressService.getProvinceById(provinceId);
        return ApiResponse.<ProvinceResponse>builder()
                .data(province)
                .build();
    }

    @GetMapping("/provinces/search")
    @Operation(
        summary = "Search provinces by keyword",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProvinceResponse.class))
                )
            )
        }
    )
    ApiResponse<List<ProvinceResponse>> searchProvinces(@RequestParam String q) {
        List<ProvinceResponse> provinces = addressService.searchProvinces(q);
        return ApiResponse.<List<ProvinceResponse>>builder()
                .data(provinces)
                .build();
    }

    // District endpoints
    @GetMapping("/provinces/{provinceId}/districts")
    @Operation(
        summary = "List districts by province",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Districts retrieved",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = DistrictResponse.class))
                )
            )
        }
    )
    ApiResponse<List<DistrictResponse>> getDistrictsByProvince(@PathVariable Long provinceId) {
        List<DistrictResponse> districts = addressService.getDistrictsByProvinceId(provinceId);
        return ApiResponse.<List<DistrictResponse>>builder()
                .data(districts)
                .build();
    }

    @GetMapping("/districts/{districtId}")
    @Operation(
        summary = "Get district by ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "District retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DistrictResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "District not found"
            )
        }
    )
    ApiResponse<DistrictResponse> getDistrictById(@PathVariable Long districtId) {
        DistrictResponse district = addressService.getDistrictById(districtId);
        return ApiResponse.<DistrictResponse>builder()
                .data(district)
                .build();
    }

    // Ward endpoints
    @GetMapping("/districts/{districtId}/wards")
    @Operation(
        summary = "List wards by district",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Wards retrieved",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = WardResponse.class))
                )
            )
        }
    )
    ApiResponse<List<WardResponse>> getWardsByDistrict(@PathVariable Long districtId) {
        List<WardResponse> wards = addressService.getWardsByDistrictId(districtId);
        return ApiResponse.<List<WardResponse>>builder()
                .data(wards)
                .build();
    }

    @GetMapping("/wards/{wardId}")
    @Operation(
        summary = "Get ward by ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Ward retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WardResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Ward not found"
            )
        }
    )
    ApiResponse<WardResponse> getWardById(@PathVariable Long wardId) {
        WardResponse ward = addressService.getWardById(wardId);
        return ApiResponse.<WardResponse>builder()
                .data(ward)
                .build();
    }

    // Street endpoints
    @GetMapping("/wards/{wardId}/streets")
    @Operation(
        summary = "List streets by ward",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Streets retrieved",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = StreetResponse.class))
                )
            )
        }
    )
    ApiResponse<List<StreetResponse>> getStreetsByWard(@PathVariable Long wardId) {
        List<StreetResponse> streets = addressService.getStreetsByWardId(wardId);
        return ApiResponse.<List<StreetResponse>>builder()
                .data(streets)
                .build();
    }

    @GetMapping("/streets/{streetId}")
    @Operation(
        summary = "Get street by ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Street retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = StreetResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Street not found"
            )
        }
    )
    ApiResponse<StreetResponse> getStreetById(@PathVariable Long streetId) {
        StreetResponse street = addressService.getStreetById(streetId);
        return ApiResponse.<StreetResponse>builder()
                .data(street)
                .build();
    }

    // Address endpoints
    @PostMapping
    @Operation(
        summary = "Create address",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AddressCreationRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Address created",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AddressResponse.class)
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    ApiResponse<AddressResponse> createAddress(@RequestBody @Valid AddressCreationRequest request) {
        AddressResponse address = addressService.createAddress(request);
        return ApiResponse.<AddressResponse>builder()
                .data(address)
                .build();
    }

    @GetMapping("/{addressId}")
    @Operation(
        summary = "Get address by ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Address retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AddressResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Address not found"
            )
        }
    )
    ApiResponse<AddressResponse> getAddressById(@PathVariable Long addressId) {
        AddressResponse address = addressService.getAddressById(addressId);
        return ApiResponse.<AddressResponse>builder()
                .data(address)
                .build();
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search addresses",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AddressResponse.class))
                )
            )
        }
    )
    ApiResponse<List<AddressResponse>> searchAddresses(@RequestParam String q) {
        List<AddressResponse> addresses = addressService.searchAddresses(q);
        return ApiResponse.<List<AddressResponse>>builder()
                .data(addresses)
                .build();
    }

    @GetMapping("/suggest")
    @Operation(
        summary = "Suggest addresses",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Suggestions returned",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AddressResponse.class))
                )
            )
        }
    )
    ApiResponse<List<AddressResponse>> suggestAddresses(@RequestParam String q) {
        List<AddressResponse> addresses = addressService.suggestAddresses(q);
        return ApiResponse.<List<AddressResponse>>builder()
                .data(addresses)
                .build();
    }

    @GetMapping("/nearby")
    @Operation(
        summary = "Nearby addresses by coordinates",
        description = "Returns addresses within the specified radius (km) around the given latitude/longitude.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Nearby addresses",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AddressResponse.class))
                )
            )
        }
    )
    ApiResponse<List<AddressResponse>> getNearbyAddresses(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "5.0") Double radius) {
        List<AddressResponse> addresses = addressService.getNearbyAddresses(latitude, longitude, radius);
        return ApiResponse.<List<AddressResponse>>builder()
                .data(addresses)
                .build();
    }
}
