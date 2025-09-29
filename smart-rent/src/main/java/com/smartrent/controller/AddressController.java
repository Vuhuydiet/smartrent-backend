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
                description = "Lấy danh sách tỉnh thành thành công",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProvinceResponse.class)),
                    examples = @ExampleObject(
                        name = "Ví dụ danh sách tỉnh thành",
                        value = """
                            [ { "provinceId": 1, "name": "Hà Nội" }, { "provinceId": 2, "name": "Thành phố Hồ Chí Minh" } ]
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
        summary = "Lấy thông tin tỉnh theo ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy thông tin tỉnh thành công",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProvinceResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy tỉnh"
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
        summary = "Tìm kiếm tỉnh theo từ khóa",
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
        summary = "Danh sách quận huyện theo tỉnh",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh sách quận huyện thành công",
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
        summary = "Lấy thông tin quận huyện theo ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy thông tin quận huyện thành công",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DistrictResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy quận huyện"
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
        summary = "Danh sách phường xã theo quận huyện",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh sách phường xã thành công",
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
        summary = "Lấy thông tin phường xã theo ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy thông tin phường xã thành công",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WardResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy phường xã"
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
        summary = "Danh sách đường phố theo phường xã",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh sách đường phố thành công",
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
        summary = "Lấy thông tin đường phố theo ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy thông tin đường phố thành công",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = StreetResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy đường phố"
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
        summary = "Tạo địa chỉ mới",
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
                description = "Tạo địa chỉ thành công",
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
        summary = "Lấy thông tin địa chỉ theo ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy thông tin địa chỉ thành công",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AddressResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy địa chỉ"
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
        summary = "Tìm kiếm địa chỉ",
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
        summary = "Gợi ý địa chỉ",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Trả về gợi ý địa chỉ",
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
        summary = "Địa chỉ lân cận theo tọa độ",
        description = "Trả về các địa chỉ trong bán kính (km) được chỉ định xung quanh vĩ độ/kinh độ cho trước.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Danh sách địa chỉ lân cận",
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
