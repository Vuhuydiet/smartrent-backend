package com.smartrent.controller;

import com.smartrent.dto.request.ListingStatusChangeRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.service.listing.VerifyListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/admin/listings")
@Tag(name = "Admin Listing Management", description = "Admin APIs for managing listing verification and status")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "Bearer Authentication")
public class VerifyListingController {

    VerifyListingService verifyListingService;

    @PutMapping("/{listingId}/status")
    @PreAuthorize("authentication.authorities.stream().anyMatch(a -> a.authority.startsWith('ROLE_ADMIN') || a.authority.startsWith('ROLE_SUPER_ADMIN'))")
    @Operation(
        summary = "Change listing verification status",
        description = "Updates the verification status of a listing. Only admin users can perform this operation.",
        parameters = {
            @Parameter(
                name = "listingId",
                description = "The ID of the listing to update",
                required = true,
                example = "123"
            )
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Listing status change request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingStatusChangeRequest.class),
                examples = @ExampleObject(
                    name = "Verify Listing",
                    value = """
                        {
                          "verified": true,
                          "reason": "Listing meets all verification requirements"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Listing status updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "code": "999999",
                          "message": "Listing status updated successfully",
                          "data": {
                            "listingId": 123,
                            "title": "Beautiful 2BR Apartment",
                            "verified": true,
                            "isVerify": true,
                            "userId": "user123",
                            "price": 1500.00,
                            "listingType": "RENT"
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
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Not Found Response",
                    value = """
                        {
                          "code": "4003",
                          "message": "Listing not found",
                          "data": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Forbidden Response",
                    value = """
                        {
                          "code": "6001",
                          "message": "Don't have permission",
                          "data": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Unauthorized Response",
                    value = """
                        {
                          "code": "5001",
                          "message": "Unauthenticated",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    public ApiResponse<ListingResponseWithAdmin> changeListingStatus(
            @PathVariable Long listingId,
            @Valid @RequestBody ListingStatusChangeRequest request) {

        log.info("Admin requesting status change for listing {} to verified: {}",
                listingId, request.getVerified());

        ListingResponseWithAdmin updatedListing = verifyListingService.changeListingStatus(listingId, request);

        return ApiResponse.<ListingResponseWithAdmin>builder()
                .message("Listing status updated successfully")
                .data(updatedListing)
                .build();
    }
}
