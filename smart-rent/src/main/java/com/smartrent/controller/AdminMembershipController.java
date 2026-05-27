package com.smartrent.controller;

import com.smartrent.dto.request.MembershipPackageUpdateRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.MembershipPackageResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.service.membership.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/admin/memberships")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin - Membership Management", description = "Admin APIs for managing membership packages")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminMembershipController {

        MembershipService membershipService;

        @GetMapping("/packages")
        @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_UA', 'ROLE_SPA')")
        @Operation(summary = "List all membership packages (Admin)", description = "Returns all membership packages including inactive ones, with pagination. "
                        +
                        "Use this endpoint in the admin console so administrators can see and re-enable packages that have been deactivated.", responses = {
                                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved packages", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "Success Response", value = """
                                                        {
                                                          "code": "999999",
                                                          "message": null,
                                                          "data": {
                                                            "page": 1,
                                                            "size": 10,
                                                            "totalElements": 3,
                                                            "totalPages": 1,
                                                            "data": [
                                                              {
                                                                "membershipId": 1,
                                                                "packageCode": "BASIC_1M",
                                                                "packageName": "Basic Monthly",
                                                                "packageLevel": "BASIC",
                                                                "isActive": false,
                                                                "benefits": []
                                                              }
                                                            ]
                                                          }
                                                        }
                                                        """)))
                        })
        public ApiResponse<PageResponse<MembershipPackageResponse>> getAllPackages(
                        @Parameter(description = "Page number (1-indexed)", example = "1") @RequestParam(defaultValue = "1") int page,
                        @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size) {
                log.info("Admin listing all membership packages - page: {}, size: {}", page, size);
                PageResponse<MembershipPackageResponse> packages = membershipService.getAllPackages(page, size);
                return ApiResponse.<PageResponse<MembershipPackageResponse>>builder()
                                .data(packages)
                                .build();
        }

        @PutMapping("/packages/{membershipId}")
        @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_UA', 'ROLE_SPA')")
        @Operation(summary = "Update membership package (Admin)", description = "Updates an existing membership package.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = MembershipPackageUpdateRequest.class), examples = @ExampleObject(name = "Update Package Request", value = """
                        {
                          "packageName": "Premium 12 Months - Updated",
                          "salePrice": 8990000,
                          "discountPercentage": 25.08,
                          "isActive": true
                        }
                        """))), responses = {
                                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Package updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MembershipPackageResponse.class))),
                                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Not Found Error", value = """
                                                        {
                                                          "code": "4015",
                                                          "message": "Membership package not found"
                                                        }
                                                        """)))
                        })
        public ApiResponse<MembershipPackageResponse> updatePackage(
                        @Parameter(description = "Membership package ID", required = true) @PathVariable Long membershipId,
                        @Valid @RequestBody MembershipPackageUpdateRequest request) {
                log.info("Admin updating membership package: {}", membershipId);
                MembershipPackageResponse response = membershipService.updatePackage(membershipId, request);
                return ApiResponse.<MembershipPackageResponse>builder()
                                .data(response)
                                .build();
        }

        @DeleteMapping("/packages/{membershipId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_UA', 'ROLE_SPA')")
        @Operation(summary = "Delete membership package (Admin)", description = "Deletes a membership package.", responses = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Package deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Not Found Error", value = """
                                        {
                                          "code": "4015",
                                          "message": "Membership package not found"
                                        }
                                        """)))
        })
        public void deletePackage(
                        @Parameter(description = "Membership package ID", required = true) @PathVariable Long membershipId) {
                log.info("Admin deleting membership package: {}", membershipId);
                membershipService.deletePackage(membershipId);
        }
}
