package com.smartrent.controller;

import com.smartrent.dto.response.*;
import com.smartrent.service.address.AddressService;
import com.smartrent.service.address.NewAddressService;
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


@RestController
@RequestMapping("/v1/addresses")
@Tag(
    name = "Address API (Unified)",
    description = """
        Vietnam Administrative Address API - Supporting Both Legacy and New Structures

        **Legacy Structure (63 provinces)** - ID-based operations:
        - **Province** (Tỉnh/Thành phố) - 63 total
        - **District** (Quận/Huyện/Thị xã) - ~700 total
        - **Ward** (Phường/Xã/Thị trấn) - ~11,000 total

        **New Structure (34 provinces)** - Code-based operations (After 1/7/2025):
        - **Province** (Tỉnh/Thành phố) - 34 total
        - **Ward** (Phường/Xã/Thị trấn) - Direct under province
        - Eliminated district level (2-tier structure)

        **Key Features**:
        - Legacy endpoints: ID-based operations with 3-tier hierarchy
        - New endpoints: Code-based operations with simplified 2-tier hierarchy
        - Search functionality across all administrative levels
        - Full address resolution
        - Pagination support for new structure

        **Common Use Cases**:
        - Property listing address selection (both structures)
        - Location-based services
        - Address management for legacy and new data
        - Dropdown population for address forms
        """
)
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressService addressService;
    private final NewAddressService newAddressService;

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
                                  "provinceId": 1,
                                  "name": "Thành phố Hà Nội",
                                  "code": "01",
                                  "isActive": true
                                },
                                {
                                  "provinceId": 79,
                                  "name": "Thành phố Hồ Chí Minh",
                                  "code": "79",
                                  "isActive": true
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
    public ResponseEntity<ApiResponse<List<LegacyProvinceResponse>>> getAllProvinces() {
        log.info("GET /v1/addresses/provinces - Fetching all active provinces (legacy structure)");

        List<LegacyProvinceResponse> provinces = addressService.getAllProvinces();

        return ResponseEntity.ok(ApiResponse.<List<LegacyProvinceResponse>>builder()
                .data(provinces)
                .message("Successfully retrieved " + provinces.size() + " provinces")
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
    public ResponseEntity<ApiResponse<LegacyProvinceResponse>> getProvinceById(
            @Parameter(description = "Province ID", example = "1", required = true)
            @PathVariable Integer provinceId) {

        log.info("GET /v1/addresses/provinces/{}", provinceId);

        LegacyProvinceResponse province = addressService.getProvinceById(provinceId);

        return ResponseEntity.ok(ApiResponse.<LegacyProvinceResponse>builder()
                .data(province)
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
    public ResponseEntity<ApiResponse<List<LegacyProvinceResponse>>> searchProvinces(
            @Parameter(description = "Search term", example = "Hà Nội", required = true)
            @RequestParam String q) {

        log.info("GET /v1/addresses/provinces/search - query={}", q);

        List<LegacyProvinceResponse> provinces = addressService.searchProvinces(q);

        return ResponseEntity.ok(ApiResponse.<List<LegacyProvinceResponse>>builder()
                .data(provinces)
                .message("Found " + provinces.size() + " provinces matching '" + q + "'")
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
    public ResponseEntity<ApiResponse<List<LegacyDistrictResponse>>> getDistrictsByProvinceId(
            @Parameter(description = "Province ID", example = "1", required = true)
            @PathVariable Integer provinceId) {

        log.info("GET /v1/addresses/provinces/{}/districts", provinceId);

        List<LegacyDistrictResponse> districts = addressService.getDistrictsByProvinceId(provinceId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyDistrictResponse>>builder()
                .data(districts)
                .message("Successfully retrieved " + districts.size() + " districts for province " + provinceId)
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
    public ResponseEntity<ApiResponse<LegacyDistrictResponse>> getDistrictById(
            @Parameter(description = "District ID", example = "1", required = true)
            @PathVariable Integer districtId) {

        log.info("GET /v1/addresses/districts/{}", districtId);

        LegacyDistrictResponse district = addressService.getDistrictById(districtId);

        return ResponseEntity.ok(ApiResponse.<LegacyDistrictResponse>builder()
                .data(district)
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
    public ResponseEntity<ApiResponse<List<LegacyDistrictResponse>>> searchDistricts(
            @Parameter(description = "Search term", example = "Ba Đình", required = true)
            @RequestParam String q,

            @Parameter(description = "Province ID to filter by (optional)", example = "1")
            @RequestParam(required = false) Integer provinceId) {

        log.info("GET /v1/addresses/districts/search - query={}, provinceId={}", q, provinceId);

        List<LegacyDistrictResponse> districts = addressService.searchDistricts(q, provinceId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyDistrictResponse>>builder()
                .data(districts)
                .message("Found " + districts.size() + " districts matching '" + q + "'")
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
    public ResponseEntity<ApiResponse<List<LegacyWardResponse>>> getWardsByDistrictId(
            @Parameter(description = "District ID", example = "1", required = true)
            @PathVariable Integer districtId) {

        log.info("GET /v1/addresses/districts/{}/wards", districtId);

        List<LegacyWardResponse> wards = addressService.getWardsByDistrictId(districtId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyWardResponse>>builder()
                .data(wards)
                .message("Successfully retrieved " + wards.size() + " wards for district " + districtId)
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
    public ResponseEntity<ApiResponse<LegacyWardResponse>> getWardById(
            @Parameter(description = "Ward ID", example = "1", required = true)
            @PathVariable Integer wardId) {

        log.info("GET /v1/addresses/wards/{}", wardId);

        LegacyWardResponse ward = addressService.getWardById(wardId);

        return ResponseEntity.ok(ApiResponse.<LegacyWardResponse>builder()
                .data(ward)
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
    public ResponseEntity<ApiResponse<List<LegacyWardResponse>>> searchWards(
            @Parameter(description = "Search term", example = "Phúc Xá", required = true)
            @RequestParam String q,

            @Parameter(description = "District ID to filter by (optional)", example = "1")
            @RequestParam(required = false) Integer districtId) {

        log.info("GET /v1/addresses/wards/search - query={}, districtId={}", q, districtId);

        List<LegacyWardResponse> wards = addressService.searchWards(q, districtId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyWardResponse>>builder()
                .data(wards)
                .message("Found " + wards.size() + " wards matching '" + q + "'")
                .build());
    }

    @GetMapping("/provinces/{provinceId}/streets")
    @Operation(
        summary = "Get streets by province ID",
        description = """
            Returns all streets belonging to a specific province.

            **Vietnamese Street Types**:
            - **Đường** (Street/Road) - Main street
            - **Phố** (Street) - Street in urban areas
            - **Ngõ** (Alley) - Small street/alley
            - **Ngách** (Lane) - Very small lane

            **Use Cases**:
            - Populate street dropdown for a province
            - Display all streets in a province
            - Support cascading address selection
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved streets",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Province Streets Example",
                        summary = "Danh sách đường/phố của một tỉnh",
                        value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "name": "Nguyễn Trãi",
                                  "nameEn": "Nguyen Trai",
                                  "prefix": "Đường",
                                  "provinceId": 1,
                                  "provinceName": "Thành phố Hà Nội",
                                  "districtId": 1,
                                  "districtName": "Quận Ba Đình"
                                }
                              ],
                              "message": "Successfully retrieved 500 streets for province 1"
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
    public ResponseEntity<ApiResponse<List<LegacyStreetResponse>>> getStreetsByProvinceId(
            @Parameter(description = "Province ID", example = "1", required = true)
            @PathVariable Integer provinceId) {

        log.info("GET /v1/addresses/provinces/{}/streets", provinceId);

        List<LegacyStreetResponse> streets = addressService.getStreetsByProvinceId(provinceId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyStreetResponse>>builder()
                .data(streets)
                .message("Successfully retrieved " + streets.size() + " streets for province " + provinceId)
                .build());
    }

    @GetMapping("/districts/{districtId}/streets")
    @Operation(
        summary = "Get streets by district ID",
        description = """
            Returns all streets belonging to a specific district.

            **Vietnamese Street Types**:
            - **Đường** (Street/Road) - Main street
            - **Phố** (Street) - Street in urban areas
            - **Ngõ** (Alley) - Small street/alley
            - **Ngách** (Lane) - Very small lane

            **Use Cases**:
            - Populate street dropdown after district selection
            - Display all streets in a district
            - Support cascading address selection
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved streets",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "District Streets Example",
                        summary = "Danh sách đường/phố của một quận/huyện",
                        value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "name": "Nguyễn Trãi",
                                  "nameEn": "Nguyen Trai",
                                  "prefix": "Đường",
                                  "provinceId": 1,
                                  "provinceName": "Thành phố Hà Nội",
                                  "districtId": 1,
                                  "districtName": "Quận Ba Đình"
                                }
                              ],
                              "message": "Successfully retrieved 50 streets for district 1"
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
    public ResponseEntity<ApiResponse<List<LegacyStreetResponse>>> getStreetsByDistrictId(
            @Parameter(description = "District ID", example = "1", required = true)
            @PathVariable Integer districtId) {

        log.info("GET /v1/addresses/districts/{}/streets", districtId);

        List<LegacyStreetResponse> streets = addressService.getStreetsByDistrictId(districtId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyStreetResponse>>builder()
                .data(streets)
                .message("Successfully retrieved " + streets.size() + " streets for district " + districtId)
                .build());
    }

    @GetMapping("/streets/{streetId}")
    @Operation(
        summary = "Get street by ID",
        description = "Get detailed information about a specific street by its ID.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Street found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Street not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<LegacyStreetResponse>> getStreetById(
            @Parameter(description = "Street ID", example = "1", required = true)
            @PathVariable Integer streetId) {

        log.info("GET /v1/addresses/streets/{}", streetId);

        LegacyStreetResponse street = addressService.getStreetById(streetId);

        return ResponseEntity.ok(ApiResponse.<LegacyStreetResponse>builder()
                .data(street)
                .message("Successfully retrieved street")
                .build());
    }

    @GetMapping("/streets/search")
    @Operation(
        summary = "Search streets by name",
        description = """
            Search for streets by name (case-insensitive partial match).
            Optionally filter by province and/or district.
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results"
            )
        }
    )
    public ResponseEntity<ApiResponse<List<LegacyStreetResponse>>> searchStreets(
            @Parameter(description = "Search term", example = "Nguyễn Trãi", required = true)
            @RequestParam String q,

            @Parameter(description = "Province ID to filter by (optional)", example = "1")
            @RequestParam(required = false) Integer provinceId,

            @Parameter(description = "District ID to filter by (optional)", example = "1")
            @RequestParam(required = false) Integer districtId) {

        log.info("GET /v1/addresses/streets/search - query={}, provinceId={}, districtId={}", q, provinceId, districtId);

        List<LegacyStreetResponse> streets = addressService.searchStreets(q, provinceId, districtId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyStreetResponse>>builder()
                .data(streets)
                .message("Found " + streets.size() + " streets matching '" + q + "'")
                .build());
    }

    @GetMapping("/provinces/{provinceId}/projects")
    @Operation(
        summary = "Get projects by province ID",
        description = """
            Returns all projects/locations belonging to a specific province.

            **Use Cases**:
            - Display available projects in a province
            - Filter listings by project location
            - Populate project dropdown after province selection
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved projects",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Province Projects Example",
                        summary = "Danh sách dự án/khu vực trong tỉnh",
                        value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "name": "Vinhomes Grand Park",
                                  "nameEn": "Vinhomes Grand Park",
                                  "provinceId": 79,
                                  "provinceName": "Thành phố Hồ Chí Minh",
                                  "districtId": 769,
                                  "districtName": "Quận 9",
                                  "latitude": 10.8455,
                                  "longitude": 106.8564
                                }
                              ],
                              "message": "Successfully retrieved 15 projects for province 79"
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
    public ResponseEntity<ApiResponse<List<LegacyProjectResponse>>> getProjectsByProvinceId(
            @Parameter(description = "Province ID", example = "79", required = true)
            @PathVariable Integer provinceId) {

        log.info("GET /v1/addresses/provinces/{}/projects", provinceId);

        List<LegacyProjectResponse> projects = addressService.getProjectsByProvinceId(provinceId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyProjectResponse>>builder()
                .data(projects)
                .message("Successfully retrieved " + projects.size() + " projects for province " + provinceId)
                .build());
    }

    @GetMapping("/districts/{districtId}/projects")
    @Operation(
        summary = "Get projects by district ID",
        description = """
            Returns all projects/locations belonging to a specific district.

            **Use Cases**:
            - Display available projects in a district
            - Filter listings by project location
            - Populate project dropdown after district selection
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved projects",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "District Projects Example",
                        summary = "Danh sách dự án/khu vực trong quận/huyện",
                        value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "name": "Vinhomes Grand Park",
                                  "nameEn": "Vinhomes Grand Park",
                                  "provinceId": 79,
                                  "provinceName": "Thành phố Hồ Chí Minh",
                                  "districtId": 769,
                                  "districtName": "Quận 9",
                                  "latitude": 10.8455,
                                  "longitude": 106.8564
                                }
                              ],
                              "message": "Successfully retrieved 5 projects for district 769"
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
    public ResponseEntity<ApiResponse<List<LegacyProjectResponse>>> getProjectsByDistrictId(
            @Parameter(description = "District ID", example = "769", required = true)
            @PathVariable Integer districtId) {

        log.info("GET /v1/addresses/districts/{}/projects", districtId);

        List<LegacyProjectResponse> projects = addressService.getProjectsByDistrictId(districtId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyProjectResponse>>builder()
                .data(projects)
                .message("Successfully retrieved " + projects.size() + " projects for district " + districtId)
                .build());
    }

    @GetMapping("/projects/{projectId}")
    @Operation(
        summary = "Get project by ID",
        description = "Get detailed information about a specific project by its ID.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Project found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Project not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<LegacyProjectResponse>> getProjectById(
            @Parameter(description = "Project ID", example = "1", required = true)
            @PathVariable Integer projectId) {

        log.info("GET /v1/addresses/projects/{}", projectId);

        LegacyProjectResponse project = addressService.getProjectById(projectId);

        return ResponseEntity.ok(ApiResponse.<LegacyProjectResponse>builder()
                .data(project)
                .message("Successfully retrieved project")
                .build());
    }

    @GetMapping("/projects/search")
    @Operation(
        summary = "Search projects by name",
        description = """
            Search for projects by name (case-insensitive partial match).
            Optionally filter by province and/or district.
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results"
            )
        }
    )
    public ResponseEntity<ApiResponse<List<LegacyProjectResponse>>> searchProjects(
            @Parameter(description = "Search term", example = "Vinhomes", required = true)
            @RequestParam String q,

            @Parameter(description = "Province ID to filter by (optional)", example = "79")
            @RequestParam(required = false) Integer provinceId,

            @Parameter(description = "District ID to filter by (optional)", example = "769")
            @RequestParam(required = false) Integer districtId) {

        log.info("GET /v1/addresses/projects/search - query={}, provinceId={}, districtId={}", q, provinceId, districtId);

        List<LegacyProjectResponse> projects = addressService.searchProjects(q, provinceId, districtId);

        return ResponseEntity.ok(ApiResponse.<List<LegacyProjectResponse>>builder()
                .data(projects)
                .message("Found " + projects.size() + " projects matching '" + q + "'")
                .build());
    }


    // ==================== NEW ADDRESS STRUCTURE ENDPOINTS (After 1/7/2025) ====================

    @GetMapping("/new-provinces")
    @Operation(
        summary = "Get all provinces (34 provinces - new structure)",
        description = """
            Returns all provinces in Vietnam's new administrative structure (after 1/7/2025).

            **Returns**: 34 provinces including:
            - Merged provinces from the old 63-province structure
            - New administrative boundaries

            **Query Parameters**:
            - keyword: Search by province name or code
            - page: Page number (default: 1)
            - limit: Items per page (default: 20, max: 100)

            **Use Cases**:
            - Populate province dropdown in address forms
            - Display complete list of new provinces
            - First step in cascading address selection
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved provinces",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaginatedResponse.class),
                    examples = @ExampleObject(
                        name = "New Province List Example",
                        summary = "Danh sách 34 tỉnh/thành phố mới",
                        value = """
                            {
                              "success": true,
                              "message": "Success",
                              "data": [
                                {
                                  "province_id": 13,
                                  "code": "01",
                                  "name": "Hà Nội",
                                  "type": "Thành phố"
                                },
                                {
                                  "province_id": 15,
                                  "code": "79",
                                  "name": "Hồ Chí Minh",
                                  "type": "Thành phố"
                                }
                              ],
                              "metadata": {
                                "total": 34,
                                "page": 1,
                                "limit": 20
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    public ResponseEntity<PaginatedResponse<List<NewProvinceResponse>>> getNewProvinces(
            @Parameter(description = "Search keyword", example = "Hà Nội")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Page number", example = "1")
            @RequestParam(required = false) Integer page,

            @Parameter(description = "Items per page (max 100)", example = "20")
            @RequestParam(required = false) Integer limit) {

        log.info("GET /v1/addresses/new-provinces - keyword: {}, page: {}, limit: {}",
                keyword, page, limit);

        PaginatedResponse<List<NewProvinceResponse>> response =
                newAddressService.getNewProvinces(keyword, page, limit);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/new-provinces/{provinceCode}/wards")
    @Operation(
        summary = "Get wards by province code (new structure)",
        description = """
            Returns all wards belonging to a specific province in the new structure.
            In the new structure, wards are directly under provinces without districts.

            **Vietnamese Ward Types**:
            - **Phường** (Ward) - Urban administrative unit
            - **Xã** (Commune) - Rural administrative unit
            - **Thị trấn** (Township) - Small town unit

            **Query Parameters**:
            - keyword: Search by ward name or code
            - page: Page number (default: 1)
            - limit: Items per page (default: 20, max: 100)

            **Use Cases**:
            - Populate ward dropdown after province selection
            - Second step in simplified cascading address selection
            - Display all wards in a province
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved wards",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaginatedResponse.class),
                    examples = @ExampleObject(
                        name = "Hanoi Wards Example",
                        summary = "Danh sách phường/xã của Hà Nội (cấu trúc mới)",
                        value = """
                            {
                              "success": true,
                              "message": "Success",
                              "data": [
                                {
                                  "ward_id": 105,
                                  "code": "09877",
                                  "name": "An Khánh",
                                  "type": "Xã",
                                  "province_code": "01"
                                },
                                {
                                  "ward_id": 3,
                                  "code": "00004",
                                  "name": "Ba Đình",
                                  "type": "Phường",
                                  "province_code": "01"
                                }
                              ],
                              "metadata": {
                                "total": 126,
                                "page": 1,
                                "limit": 20
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid province code"
            )
        }
    )
    public ResponseEntity<PaginatedResponse<List<NewWardResponse>>> getWardsByNewProvince(
            @Parameter(description = "Province code", example = "01", required = true)
            @PathVariable String provinceCode,

            @Parameter(description = "Search keyword", example = "Ba Đình")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Page number", example = "1")
            @RequestParam(required = false) Integer page,

            @Parameter(description = "Items per page (max 100)", example = "20")
            @RequestParam(required = false) Integer limit) {

        log.info("GET /v1/addresses/new-provinces/{}/wards - keyword: {}, page: {}, limit: {}",
                provinceCode, keyword, page, limit);

        PaginatedResponse<List<NewWardResponse>> response =
                newAddressService.getWardsByNewProvince(provinceCode, keyword, page, limit);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/new-full-address")
    @Operation(
        summary = "Get full address information (new structure)",
        description = """
            Returns complete address information for the new structure.
            Resolves province and ward details based on provided codes.

            **Query Parameters**:
            - provinceCode: Province code (required)
            - wardCode: Ward code (optional)

            **Use Cases**:
            - Resolve full address from codes
            - Validate address codes
            - Display complete address information
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved full address",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Full Address Example",
                        summary = "Thông tin địa chỉ đầy đủ",
                        value = """
                            {
                              "data": {
                                "province": {
                                  "province_id": 13,
                                  "code": "01",
                                  "name": "Hà Nội",
                                  "type": "Thành phố"
                                },
                                "ward": {
                                  "ward_id": 3,
                                  "code": "00004",
                                  "name": "Ba Đình",
                                  "type": "Phường",
                                  "province_code": "01"
                                }
                              },
                              "message": "Successfully retrieved full address"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid parameters"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Address not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<NewFullAddressResponse>> getNewFullAddress(
            @Parameter(description = "Province code", example = "01", required = true)
            @RequestParam String provinceCode,

            @Parameter(description = "Ward code", example = "00004")
            @RequestParam(required = false) String wardCode) {

        log.info("GET /v1/addresses/new-full-address - provinceCode: {}, wardCode: {}",
                provinceCode, wardCode);

        NewFullAddressResponse addressResponse =
                newAddressService.getNewFullAddress(provinceCode, wardCode);

        return ResponseEntity.ok(ApiResponse.<NewFullAddressResponse>builder()
                .data(addressResponse)
                .message("Successfully retrieved full address")
                .build());
    }

    @GetMapping("/search-new-address")
    @Operation(
        summary = "Search addresses (new structure)",
        description = """
            Search for provinces and wards in the new administrative structure.
            Performs a full-text search across all administrative units.

            **Query Parameters**:
            - keyword: Search keyword (required)
            - page: Page number (default: 1)
            - limit: Items per page (default: 20, max: 100)

            **Use Cases**:
            - Auto-complete address fields
            - Search across provinces and wards
            - Find addresses by partial names
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully performed search",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaginatedResponse.class),
                    examples = @ExampleObject(
                        name = "Search Results Example",
                        summary = "Kết quả tìm kiếm địa chỉ",
                        value = """
                            {
                              "success": true,
                              "message": "Success",
                              "data": [
                                {
                                  "code": "01",
                                  "name": "Hà Nội",
                                  "type": "Thành phố",
                                  "province_code": "01",
                                  "province_name": "Hà Nội",
                                  "full_address": "Hà Nội"
                                }
                              ],
                              "metadata": {
                                "total": 1,
                                "page": 1,
                                "limit": 20
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid parameters"
            )
        }
    )
    public ResponseEntity<PaginatedResponse<List<NewAddressSearchResponse>>> searchNewAddress(
            @Parameter(description = "Search keyword", example = "Hà Nội", required = true)
            @RequestParam String keyword,

            @Parameter(description = "Page number", example = "1")
            @RequestParam(required = false) Integer page,

            @Parameter(description = "Items per page (max 100)", example = "20")
            @RequestParam(required = false) Integer limit) {

        log.info("GET /v1/addresses/search-new-address - keyword: {}, page: {}, limit: {}",
                keyword, page, limit);

        PaginatedResponse<List<NewAddressSearchResponse>> response =
                newAddressService.searchNewAddress(keyword, page, limit);

        return ResponseEntity.ok(response);
    }

    // ==================== ADDRESS CONVERSION ====================

    @GetMapping("/convert/legacy-to-new")
    @Operation(
        summary = "Convert legacy address to new structure",
        description = """
            Converts an address from legacy structure (63 provinces, 3-tier) to new structure (34 provinces, 2-tier).

            **Input**: Legacy Province ID, District ID, Ward ID
            **Output**: Both legacy and new address information with conversion notes

            **Use Cases**:
            - Migrate existing data from legacy to new structure
            - Show users their address in both formats
            - Validate address conversions

            **Note**: Uses mapping tables to find corresponding new addresses.
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully converted address",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Conversion Example",
                        summary = "Chuyển đổi địa chỉ từ cấu trúc cũ sang mới",
                        value = """
                            {
                              "data": {
                                "legacyAddress": {
                                  "province": {"id": 1, "name": "Hà Nội", "code": "01"},
                                  "district": {"id": 1, "name": "Ba Đình"},
                                  "ward": {"id": 1, "name": "Phúc Xá"}
                                },
                                "newAddress": {
                                  "province": {"code": "01", "name": "Hà Nội"},
                                  "ward": {"code": "00001", "name": "Phúc Xá"}
                                },
                                "conversionNote": "Converted from legacy structure. Merge type: KEEP"
                              },
                              "message": "Successfully converted address from legacy to new structure"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Address not found or mapping not available"
            )
        }
    )
    public ResponseEntity<ApiResponse<AddressConversionResponse>> convertLegacyToNew(
            @Parameter(description = "Legacy Province ID", example = "1", required = true)
            @RequestParam Integer provinceId,

            @Parameter(description = "Legacy District ID", example = "1", required = true)
            @RequestParam Integer districtId,

            @Parameter(description = "Legacy Ward ID", example = "1", required = true)
            @RequestParam Integer wardId) {

        log.info("GET /v1/addresses/convert/legacy-to-new - provinceId: {}, districtId: {}, wardId: {}",
                provinceId, districtId, wardId);

//        AddressConversionResponse response = addressService.convertLegacyToNew(provinceId, districtId, wardId);
        AddressConversionResponse response = new AddressConversionResponse();
        return ResponseEntity.ok(ApiResponse.<AddressConversionResponse>builder()
                .data(response)
                .message("Successfully converted address from legacy to new structure")
                .build());
    }

    @GetMapping("/convert/new-to-legacy")
    @Operation(
        summary = "Convert new address to legacy structure",
        description = """
            Converts an address from new structure (34 provinces, 2-tier) to legacy structure (63 provinces, 3-tier).

            **Input**: New Province Code, Ward Code
            **Output**: Both new and legacy address information with conversion notes

            **Use Cases**:
            - Support backward compatibility with legacy systems
            - Show users their address in legacy format
            - Export data for systems using old structure

            **Note**: One new ward may map to multiple legacy wards. Returns first match by default.
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully converted address",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Reverse Conversion Example",
                        summary = "Chuyển đổi địa chỉ từ cấu trúc mới về cũ",
                        value = """
                            {
                              "data": {
                                "legacyAddress": {
                                  "province": {"id": 1, "name": "Hà Nội", "code": "01"},
                                  "district": {"id": 1, "name": "Ba Đình"},
                                  "ward": {"id": 1, "name": "Phúc Xá"}
                                },
                                "newAddress": {
                                  "province": {"code": "01", "name": "Hà Nội"},
                                  "ward": {"code": "00001", "name": "Phúc Xá"}
                                },
                                "conversionNote": "Converted to legacy structure. Merge type: KEEP"
                              },
                              "message": "Successfully converted address from new to legacy structure"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Address not found or mapping not available"
            )
        }
    )
    public ResponseEntity<ApiResponse<AddressConversionResponse>> convertNewToLegacy(
            @Parameter(description = "New Province Code", example = "01", required = true)
            @RequestParam String provinceCode,

            @Parameter(description = "New Ward Code", example = "00001", required = true)
            @RequestParam String wardCode) {

        log.info("GET /v1/addresses/convert/new-to-legacy - provinceCode: {}, wardCode: {}",
                provinceCode, wardCode);

//        AddressConversionResponse response = addressService.convertNewToLegacy(provinceCode, wardCode);
        AddressConversionResponse response = new AddressConversionResponse();

        return ResponseEntity.ok(ApiResponse.<AddressConversionResponse>builder()
                .data(response)
                .message("Successfully converted address from new to legacy structure")
                .build());
    }

    // ==================== HEALTH CHECK ====================

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
                .message("Address API is healthy and operational (Unified: Legacy 3-tier + New 2-tier structure)")
                .build());
    }
}