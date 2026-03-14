package com.smartrent.controller;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.response.*;
import com.smartrent.service.listing.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Listings - Admin",
    description = """
        Admin listing management. All endpoints require `X-Admin-Id` header.

        **Endpoints:**
        - `GET /{id}/admin` - Get listing with admin verification info (verificationStatus, adminName, notes)
        - `POST /admin/list` - Paginated admin listing view with filters and dashboard statistics

        **Dashboard statistics:** pendingVerification, verified, expired, rejected counts, VIP tier breakdown.

        **Common filter combinations:**
        - Pending review: `{"verified": false, "isVerify": true}`
        - By moderation status: `{"moderationStatus": "PENDING_REVIEW"}`
        - By VIP tier: `{"vipType": "GOLD"}`
        """
)
@RequiredArgsConstructor
public class ListingAdminController {

    private final ListingService listingService;

    @GetMapping("/{id}/admin")
    @Operation(
        summary = "Get listing with admin verification info (Admin only)",
        description = "Retrieves listing details including admin verification information and status. This endpoint is for administrators only.",
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true),
            @Parameter(name = "X-Admin-Id", description = "Admin ID from authentication header", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing found with admin verification info",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
        }
    )
    public ApiResponse<ListingResponseWithAdmin> getListingByIdWithAdmin(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Id") String adminId) {
        ListingResponseWithAdmin response = listingService.getListingByIdWithAdmin(id, adminId);
        return ApiResponse.<ListingResponseWithAdmin>builder().data(response).build();
    }

    @PostMapping("/admin/list")
    @Operation(
        summary = "Get all listings for admin with filters and statistics",
        description = """
            Admin endpoint to retrieve listings with verification info and dashboard statistics.

            **Key Features:**
            - Filter by verification status, VIP type, location, price, etc.
            - Get statistics: pending/verified/expired counts, VIP tier breakdown
            - Admin verification info always includes verificationStatus (PENDING/APPROVED/REJECTED)

            **Common Filters:**
            - `verified` + `isVerify`: Filter by admin verification status
            - `vipType`: NORMAL, SILVER, GOLD, DIAMOND
            - `listingStatus`: IN_REVIEW, DISPLAYING, EXPIRED, etc.
            - `categoryId`, `provinceId`, `userId`: Filter by category/location/owner
            """,
        parameters = {
            @Parameter(name = "X-Admin-Id", description = "Admin ID", required = true)
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Pending listings",
                        summary = "Get listings waiting for verification",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "verified": false,
                              "isVerify": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Listings pending review",
                        summary = "Get listings awaiting admin review (new + resubmitted)",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "moderationStatus": "PENDING_REVIEW"
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Success",
                content = @Content(mediaType = "application/json")
            )
        }
    )
    public ApiResponse<AdminListingListResponse> getAllListingsForAdmin(
            @RequestHeader("X-Admin-Id") String adminId,
            @Valid @RequestBody ListingFilterRequest filter) {
        filter.setIsAdminRequest(true);
        AdminListingListResponse response = listingService.getAllListingsForAdmin(filter, adminId);
        return ApiResponse.<AdminListingListResponse>builder().data(response).build();
    }
}
