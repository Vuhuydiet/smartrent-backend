package com.smartrent.controller;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.UpdateAndResubmitRequest;
import com.smartrent.dto.response.*;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.moderation.ListingModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Listings - Owner",
    description = """
        Owner-specific listing management. All endpoints require JWT authentication.

        **Endpoints:**
        - `GET /{id}/my-detail` - Detailed view with transaction info, payment info, statistics
        - `POST /my-listings` - Paginated owner listings with dashboard statistics
        - `POST /{id}/resubmit-for-review` - Resubmit rejected/revision-required listing
        - `PUT /{id}/update-and-resubmit` - Update content and resubmit in one operation

        **Owner dashboard statistics include:** drafts, pending, active, expired counts and VIP tier breakdown.
        """
)
@RequiredArgsConstructor
public class ListingOwnerController {

    private final ListingService listingService;
    private final ListingModerationService listingModerationService;

    @GetMapping("/{id}/my-detail")
    @Operation(
        summary = "Get my listing detail with owner information (Owner only)",
        description = """
            Retrieves detailed listing information with owner-specific data including:
            - Transaction details (postSource, transactionId, payment info)
            - All media (images/videos) attached to the listing
            - Complete address information
            - Listing statistics (views, contacts, etc.)
            - Verification notes and rejection reason (if any)

            This endpoint is only accessible by the listing owner.
            """,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing found with owner details"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated or not the owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
        }
    )
    public ApiResponse<ListingResponseForOwner> getMyListingDetail(@PathVariable Long id) {
        String userId = extractUserId();
        ListingResponseForOwner response = listingService.getMyListingDetail(id, userId);
        return ApiResponse.<ListingResponseForOwner>builder().data(response).build();
    }

    @PostMapping("/my-listings")
    @Operation(
        summary = "Get my listings with owner-specific information (Owner only)",
        description = """
            Retrieves paginated list of owner's listings with detailed owner-specific information including:
            - Transaction details (postSource, transactionId, payment info)
            - All media (images/videos) for each listing
            - Complete address information
            - Listing statistics (views, contacts, etc.)
            - Verification notes and rejection reason (if any)
            - Owner dashboard statistics (drafts, pending, active, expired, by VIP tier)

            User authentication is required via JWT token.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Filter Example",
                    value = """
                        {
                          "page": 0,
                          "size": 20,
                          "sortBy": "DEFAULT",
                          "listingStatus": "EXPIRING_SOON",
                          "sortDirection": "DESC",
                          "vipType": "GOLD"
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "My listings retrieved successfully with statistics"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
        }
    )
    public ApiResponse<OwnerListingListResponse> getMyListings(@Valid @RequestBody ListingFilterRequest filter) {
        String userId = extractUserId();
        OwnerListingListResponse response = listingService.getMyListings(filter, userId);
        return ApiResponse.<OwnerListingListResponse>builder().data(response).build();
    }

    @Operation(
        summary = "Resubmit listing for review",
        description = """
            Resubmit a listing that was rejected or has revision required.
            The listing must be in REJECTED or REVISION_REQUIRED moderation state.
            Only the listing owner can call this endpoint.
            """,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.smartrent.dto.request.ResubmitListingRequest.class),
                examples = @ExampleObject(
                    name = "Resubmit with notes",
                    value = """
                        {
                          "notes": "Updated listing title and added missing legal information"
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing resubmitted for review"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Listing cannot be resubmitted in current state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the listing owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
        }
    )
    @PostMapping("/{id}/resubmit-for-review")
    public ApiResponse<Void> resubmitForReview(
            @PathVariable Long id,
            @RequestBody(required = false) com.smartrent.dto.request.ResubmitListingRequest request) {
        String userId = extractUserId();
        listingModerationService.resubmitForReview(id, userId, request != null ? request : new com.smartrent.dto.request.ResubmitListingRequest());
        return ApiResponse.<Void>builder().message("Listing resubmitted for review successfully").build();
    }

    @Operation(
        summary = "Update listing and resubmit for review",
        description = """
            Update listing content and resubmit for review in a single operation.
            The listing must be in REJECTED or REVISION_REQUIRED moderation state.
            Only the listing owner can call this endpoint.

            **Use cases**:
            1. Owner updates listing after admin resolves a listing report
            2. Owner updates listing after admin rejects during moderation review
            """,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true)
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdateAndResubmitRequest.class),
                examples = @ExampleObject(
                    name = "Update and resubmit",
                    value = """
                        {
                          "title": "Updated listing title as requested",
                          "description": "Updated description with corrected information",
                          "mediaIds": [101, 102, 103],
                          "notes": "Fixed title and added missing photos per admin feedback"
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing updated and resubmitted for review"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Listing cannot be resubmitted in current state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the listing owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
        }
    )
    @PutMapping("/{id}/update-and-resubmit")
    public ApiResponse<Void> updateAndResubmitForReview(
            @PathVariable Long id,
            @RequestBody UpdateAndResubmitRequest request) {
        String userId = extractUserId();
        listingModerationService.updateAndResubmitForReview(id, request, userId);
        return ApiResponse.<Void>builder().message("Listing updated and resubmitted for review successfully").build();
    }

    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return authentication.getName();
    }
}
