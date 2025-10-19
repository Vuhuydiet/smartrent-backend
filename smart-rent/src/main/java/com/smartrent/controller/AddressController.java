package com.smartrent.controller;

import com.smartrent.dto.response.*;
import com.smartrent.service.address.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/v1/addresses")
@Tag(
    name = "Address API (Legacy Structure)",
    description = """
        Vietnam Administrative Address API - Traditional 3-Tier Structure

        **Administrative Hierarchy** (Legacy Structure - 63 provinces):
        - **Province** (Tỉnh/Thành phố) - 63 total
        - **District** (Quận/Huyện/Thị xã) - ~700 total
        - **Ward** (Phường/Xã/Thị trấn) - ~11,000 total

        **Key Features**:
        - ID-based operations (provinceId, districtId, wardId)
        - Traditional 3-tier hierarchy
        - Search functionality across all administrative levels
        - Cascading selection support (Province → District → Ward)

        **Common Use Cases**:
        - Property listing address selection
        - Location-based services
        - Address management for legacy data
        - Dropdown population for address forms
        """
)
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/provinces")
    @Operation(
        summary = "Get all provinces (63 provinces - legacy structure)",
        description = """
            Returns all active provinces in Vietnam's traditional administrative structure.

            **Returns**: 63 provinces including:
            - 5 centrally-governed cities (Hà Nội, HCM, Đà Nẵng, Hải Phòng, Cần Thơ)
            - 58 provinces

            **Use Cases**:
            - Populate province dropdown in address forms
            - Display complete list of provinces
            - First step in cascading address selection
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved all provinces",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Province List Example",
                        summary = "Danh sách 63 tỉnh/thành phố Việt Nam",
                        value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "name": "Thành phố Hà Nội",
                                  "code": "01",
                                  "type": "CITY",
                                  "level": "PROVINCE",
                                  "isActive": true,
                                  "fullAddressText": "Thành phố Hà Nội"
                                },
                                {
                                  "id": 79,
                                  "name": "Thành phố Hồ Chí Minh",
                                  "code": "79",
                                  "type": "CITY",
                                  "level": "PROVINCE",
                                  "isActive": true,
                                  "fullAddressText": "Thành phố Hồ Chí Minh"
                                }
                              ],
                              "message": "Successfully retrieved 63 provinces"
                            }
                            """
                    )
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<List<AddressUnitDTO>>> getAllProvinces() {
        log.info("GET /v1/addresses/provinces - Fetching all active provinces (legacy structure)");

        List<ProvinceResponse> provinces = addressService.getAllProvinces();
        List<AddressUnitDTO> units = provinces.stream()
                .map(AddressUnitDTO::fromProvince)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<AddressUnitDTO>>builder()
                .data(units)
                .message("Successfully retrieved " + units.size() + " provinces")
                .build());
    }

    @GetMapping("/provinces/{provinceId}")
    @Operation(
        summary = "Get province by ID",
        description = "Get detailed information about a specific province by its ID.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Province found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Province not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<AddressUnitDTO>> getProvinceById(
            @Parameter(description = "Province ID", example = "1", required = true)
            @PathVariable Long provinceId) {

        log.info("GET /v1/addresses/provinces/{}", provinceId);

        ProvinceResponse province = addressService.getProvinceById(provinceId);
        AddressUnitDTO unit = AddressUnitDTO.fromProvince(province);

        return ResponseEntity.ok(ApiResponse.<AddressUnitDTO>builder()
                .data(unit)
                .message("Successfully retrieved province")
                .build());
    }

    @GetMapping("/provinces/search")
    @Operation(
        summary = "Search provinces by name",
        description = "Search for provinces by name (case-insensitive partial match)",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results"
            )
        }
    )
    public ResponseEntity<ApiResponse<List<AddressUnitDTO>>> searchProvinces(
            @Parameter(description = "Search term", example = "Hà Nội", required = true)
            @RequestParam String q) {

        log.info("GET /v1/addresses/provinces/search - query={}", q);

        List<ProvinceResponse> provinces = addressService.searchProvinces(q);
        List<AddressUnitDTO> units = provinces.stream()
                .map(AddressUnitDTO::fromProvince)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<AddressUnitDTO>>builder()
                .data(units)
                .message("Found " + units.size() + " provinces matching '" + q + "'")
                .build());
    }

    @GetMapping("/provinces/{provinceId}/districts")
    @Operation(
        summary = "Get districts by province ID",
        description = """
            Returns all districts belonging to a specific province.

            **Vietnamese District Types**:
            - **Quận** (Urban district) - Found in major cities
            - **Huyện** (Rural district) - Found in provinces
            - **Thị xã** (Town) - Mid-level urbanization
            - **Thành phố** (City) - Province-level cities

            **Use Cases**:
            - Populate district dropdown after province selection
            - Second step in cascading address selection
            - Display all districts in a province
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved districts",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Hanoi Districts Example",
                        summary = "Danh sách quận/huyện của Hà Nội",
                        value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "name": "Quận Ba Đình",
                                  "code": "001",
                                  "type": "DISTRICT",
                                  "level": "DISTRICT",
                                  "provinceId": 1,
                                  "provinceName": "Thành phố Hà Nội",
                                  "isActive": true,
                                  "fullAddressText": "Quận Ba Đình, Thành phố Hà Nội"
                                },
                                {
                                  "id": 2,
                                  "name": "Quận Hoàn Kiếm",
                                  "code": "002",
                                  "type": "DISTRICT",
                                  "level": "DISTRICT",
                                  "provinceId": 1,
                                  "provinceName": "Thành phố Hà Nội",
                                  "isActive": true,
                                  "fullAddressText": "Quận Hoàn Kiếm, Thành phố Hà Nội"
                                }
                              ],
                              "message": "Successfully retrieved 30 districts for province 1"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Province not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<List<AddressUnitDTO>>> getDistrictsByProvinceId(
            @Parameter(description = "Province ID", example = "1", required = true)
            @PathVariable Long provinceId) {

        log.info("GET /v1/addresses/provinces/{}/districts", provinceId);

        List<DistrictResponse> districts = addressService.getDistrictsByProvinceId(provinceId);
        List<AddressUnitDTO> units = districts.stream()
                .map(AddressUnitDTO::fromDistrict)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<AddressUnitDTO>>builder()
                .data(units)
                .message("Successfully retrieved " + units.size() + " districts for province " + provinceId)
                .build());
    }

    @GetMapping("/districts/{districtId}")
    @Operation(
        summary = "Get district by ID",
        description = "Get detailed information about a specific district by its ID.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "District found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "District not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<AddressUnitDTO>> getDistrictById(
            @Parameter(description = "District ID", example = "1", required = true)
            @PathVariable Long districtId) {

        log.info("GET /v1/addresses/districts/{}", districtId);

        DistrictResponse district = addressService.getDistrictById(districtId);
        AddressUnitDTO unit = AddressUnitDTO.fromDistrict(district);

        return ResponseEntity.ok(ApiResponse.<AddressUnitDTO>builder()
                .data(unit)
                .message("Successfully retrieved district")
                .build());
    }

    @GetMapping("/districts/search")
    @Operation(
        summary = "Search districts by name",
        description = """
            Search for districts by name (case-insensitive partial match).
            Optionally filter by province.
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results"
            )
        }
    )
    public ResponseEntity<ApiResponse<List<AddressUnitDTO>>> searchDistricts(
            @Parameter(description = "Search term", example = "Ba Đình", required = true)
            @RequestParam String q,

            @Parameter(description = "Province ID to filter by (optional)", example = "1")
            @RequestParam(required = false) Long provinceId) {

        log.info("GET /v1/addresses/districts/search - query={}, provinceId={}", q, provinceId);

        List<DistrictResponse> districts = addressService.searchDistricts(q, provinceId);
        List<AddressUnitDTO> units = districts.stream()
                .map(AddressUnitDTO::fromDistrict)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<AddressUnitDTO>>builder()
                .data(units)
                .message("Found " + units.size() + " districts matching '" + q + "'")
                .build());
    }

    @GetMapping("/districts/{districtId}/wards")
    @Operation(
        summary = "Get wards by district ID",
        description = """
            Returns all wards belonging to a specific district.

            **Vietnamese Ward Types**:
            - **Phường** (Ward) - Urban administrative unit
            - **Xã** (Commune) - Rural administrative unit
            - **Thị trấn** (Township) - Small town unit

            **Use Cases**:
            - Populate ward dropdown after district selection
            - Third step in cascading address selection
            - Display all wards in a district
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved wards",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Ba Dinh Wards Example",
                        summary = "Danh sách phường của Quận Ba Đình",
                        value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "name": "Phường Phúc Xá",
                                  "code": "00001",
                                  "type": "WARD",
                                  "level": "WARD",
                                  "districtId": 1,
                                  "districtName": "Quận Ba Đình",
                                  "provinceId": 1,
                                  "provinceName": "Thành phố Hà Nội",
                                  "isActive": true,
                                  "fullAddressText": "Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội"
                                },
                                {
                                  "id": 4,
                                  "name": "Phường Trúc Bạch",
                                  "code": "00004",
                                  "type": "WARD",
                                  "level": "WARD",
                                  "districtId": 1,
                                  "districtName": "Quận Ba Đình",
                                  "provinceId": 1,
                                  "provinceName": "Thành phố Hà Nội",
                                  "isActive": true,
                                  "fullAddressText": "Phường Trúc Bạch, Quận Ba Đình, Thành phố Hà Nội"
                                }
                              ],
                              "message": "Successfully retrieved 14 wards for district 1"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "District not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<List<AddressUnitDTO>>> getWardsByDistrictId(
            @Parameter(description = "District ID", example = "1", required = true)
            @PathVariable Long districtId) {

        log.info("GET /v1/addresses/districts/{}/wards", districtId);

        List<WardResponse> wards = addressService.getWardsByDistrictId(districtId);
        List<AddressUnitDTO> units = wards.stream()
                .map(AddressUnitDTO::fromWard)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<AddressUnitDTO>>builder()
                .data(units)
                .message("Successfully retrieved " + units.size() + " wards for district " + districtId)
                .build());
    }

    @GetMapping("/wards/{wardId}")
    @Operation(
        summary = "Get ward by ID",
        description = "Get detailed information about a specific ward by its ID.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Ward found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Ward not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<AddressUnitDTO>> getWardById(
            @Parameter(description = "Ward ID", example = "1", required = true)
            @PathVariable Long wardId) {

        log.info("GET /v1/addresses/wards/{}", wardId);

        WardResponse ward = addressService.getWardById(wardId);
        AddressUnitDTO unit = AddressUnitDTO.fromWard(ward);

        return ResponseEntity.ok(ApiResponse.<AddressUnitDTO>builder()
                .data(unit)
                .message("Successfully retrieved ward")
                .build());
    }

    @GetMapping("/wards/search")
    @Operation(
        summary = "Search wards by name",
        description = """
            Search for wards by name (case-insensitive partial match).
            Optionally filter by district.
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results"
            )
        }
    )
    public ResponseEntity<ApiResponse<List<AddressUnitDTO>>> searchWards(
            @Parameter(description = "Search term", example = "Phúc Xá", required = true)
            @RequestParam String q,

            @Parameter(description = "District ID to filter by (optional)", example = "1")
            @RequestParam(required = false) Long districtId) {

        log.info("GET /v1/addresses/wards/search - query={}, districtId={}", q, districtId);

        List<WardResponse> wards = addressService.searchWards(q, districtId);
        List<AddressUnitDTO> units = wards.stream()
                .map(AddressUnitDTO::fromWard)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<AddressUnitDTO>>builder()
                .data(units)
                .message("Found " + units.size() + " wards matching '" + q + "'")
                .build());
    }


    @GetMapping("/health")
    @Operation(
        summary = "API health check",
        description = "Simple health check endpoint to verify Address API is operational",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "API is healthy and operational"
            )
        }
    )
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        log.info("GET /v1/addresses/health - Health check");

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .data("OK")
                .message("Address API is healthy and operational (Legacy 3-tier structure)")
                .build());
    }
}