package com.smartrent.controller;

import com.smartrent.controller.dto.request.AddressCreationRequest;
import com.smartrent.controller.dto.response.*;
import com.smartrent.service.address.AddressService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/addresses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressController {

    AddressService addressService;

    // Province endpoints
    @GetMapping("/provinces")
    ApiResponse<List<ProvinceResponse>> getAllProvinces() {
        List<ProvinceResponse> provinces = addressService.getParentProvinces();
        return ApiResponse.<List<ProvinceResponse>>builder()
                .data(provinces)
                .build();
    }

    @GetMapping("/provinces/{provinceId}")
    ApiResponse<ProvinceResponse> getProvinceById(@PathVariable Long provinceId) {
        ProvinceResponse province = addressService.getProvinceById(provinceId);
        return ApiResponse.<ProvinceResponse>builder()
                .data(province)
                .build();
    }

    @GetMapping("/provinces/search")
    ApiResponse<List<ProvinceResponse>> searchProvinces(@RequestParam String q) {
        List<ProvinceResponse> provinces = addressService.searchProvinces(q);
        return ApiResponse.<List<ProvinceResponse>>builder()
                .data(provinces)
                .build();
    }

    // District endpoints
    @GetMapping("/provinces/{provinceId}/districts")
    ApiResponse<List<DistrictResponse>> getDistrictsByProvince(@PathVariable Long provinceId) {
        List<DistrictResponse> districts = addressService.getDistrictsByProvinceId(provinceId);
        return ApiResponse.<List<DistrictResponse>>builder()
                .data(districts)
                .build();
    }

    @GetMapping("/districts/{districtId}")
    ApiResponse<DistrictResponse> getDistrictById(@PathVariable Long districtId) {
        DistrictResponse district = addressService.getDistrictById(districtId);
        return ApiResponse.<DistrictResponse>builder()
                .data(district)
                .build();
    }

    // Ward endpoints
    @GetMapping("/districts/{districtId}/wards")
    ApiResponse<List<WardResponse>> getWardsByDistrict(@PathVariable Long districtId) {
        List<WardResponse> wards = addressService.getWardsByDistrictId(districtId);
        return ApiResponse.<List<WardResponse>>builder()
                .data(wards)
                .build();
    }

    @GetMapping("/wards/{wardId}")
    ApiResponse<WardResponse> getWardById(@PathVariable Long wardId) {
        WardResponse ward = addressService.getWardById(wardId);
        return ApiResponse.<WardResponse>builder()
                .data(ward)
                .build();
    }

    // Street endpoints
    @GetMapping("/wards/{wardId}/streets")
    ApiResponse<List<StreetResponse>> getStreetsByWard(@PathVariable Long wardId) {
        List<StreetResponse> streets = addressService.getStreetsByWardId(wardId);
        return ApiResponse.<List<StreetResponse>>builder()
                .data(streets)
                .build();
    }

    @GetMapping("/streets/{streetId}")
    ApiResponse<StreetResponse> getStreetById(@PathVariable Long streetId) {
        StreetResponse street = addressService.getStreetById(streetId);
        return ApiResponse.<StreetResponse>builder()
                .data(street)
                .build();
    }

    // Address endpoints
    @PostMapping
    ApiResponse<AddressResponse> createAddress(@RequestBody @Valid AddressCreationRequest request) {
        AddressResponse address = addressService.createAddress(request);
        return ApiResponse.<AddressResponse>builder()
                .data(address)
                .build();
    }

    @GetMapping("/{addressId}")
    ApiResponse<AddressResponse> getAddressById(@PathVariable Long addressId) {
        AddressResponse address = addressService.getAddressById(addressId);
        return ApiResponse.<AddressResponse>builder()
                .data(address)
                .build();
    }

    @GetMapping("/search")
    ApiResponse<List<AddressResponse>> searchAddresses(@RequestParam String q) {
        List<AddressResponse> addresses = addressService.searchAddresses(q);
        return ApiResponse.<List<AddressResponse>>builder()
                .data(addresses)
                .build();
    }

    @GetMapping("/suggest")
    ApiResponse<List<AddressResponse>> suggestAddresses(@RequestParam String q) {
        List<AddressResponse> addresses = addressService.suggestAddresses(q);
        return ApiResponse.<List<AddressResponse>>builder()
                .data(addresses)
                .build();
    }

    @GetMapping("/nearby")
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
