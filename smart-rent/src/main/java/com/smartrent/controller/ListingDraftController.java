package com.smartrent.controller;

import com.smartrent.dto.request.DraftListingRequest;
import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.*;
import com.smartrent.service.listing.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Listings - Drafts",
    description = """
        Draft listing management for auto-save and staged publishing.

        **Lifecycle:** Create draft → Auto-save updates → Publish (with payment/quota) or Delete

        **Endpoints:**
        - `POST /draft` - Create a new draft (all fields optional)
        - `POST /draft/{draftId}` - Update draft (auto-save, partial data)
        - `GET /draft/{draftId}` - Get draft by ID (owner only)
        - `GET /my-drafts` - List all user's drafts (sorted by updatedAt desc)
        - `POST /draft/{draftId}/publish` - Validate & publish draft as listing
        - `DELETE /draft/{draftId}` - Discard draft

        **All endpoints require JWT authentication.** Only the draft owner can access their drafts.
        """
)
@RequiredArgsConstructor
public class ListingDraftController {

    private final ListingService listingService;

    @Operation(
        summary = "Create draft listing",
        description = """
            Create a draft listing with all optional fields.
            Used for auto-save functionality during listing creation.

            **All fields are optional** - you can save partial data at any time.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DraftListingRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Minimal Draft",
                        summary = "Save draft with minimal data",
                        value = """
                            {
                              "title": "Căn hộ 2 phòng ngủ"
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Draft listing created successfully",
                content = @Content(mediaType = "application/json")
            )
        }
    )
    @PostMapping("/draft")
    public ApiResponse<DraftListingResponse> createDraftListing(@RequestBody DraftListingRequest request) {
        String userId = extractUserId();
        request.setUserId(userId);
        DraftListingResponse response = listingService.createDraftListing(request);
        return ApiResponse.<DraftListingResponse>builder().data(response).build();
    }

    @PostMapping("/draft/{draftId}")
    @Operation(
        summary = "Update draft listing (auto-save)",
        description = """
            Update draft listing with partial data. All fields are optional.
            This endpoint is used for auto-saving draft listings during creation.

            **Use case**: Auto-save every 30 seconds when user is creating a listing
            """,
        parameters = {
            @Parameter(name = "draftId", description = "Draft ID", required = true, example = "123")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Partial draft data to update (all fields optional)",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DraftListingRequest.class),
                examples = @ExampleObject(
                    name = "Partial Update",
                    value = """
                        {
                          "title": "Updated title",
                          "price": 6000000,
                          "bedrooms": 3
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Draft updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Draft not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not own this draft")
        }
    )
    public ApiResponse<DraftListingResponse> updateDraft(
            @PathVariable Long draftId,
            @RequestBody DraftListingRequest request) {
        String userId = extractUserId();
        DraftListingResponse response = listingService.updateDraft(draftId, request, userId);
        return ApiResponse.<DraftListingResponse>builder().data(response).build();
    }

    @GetMapping("/draft/{draftId}")
    @Operation(
        summary = "Get draft listing by ID",
        description = """
            Get a specific draft listing by its ID.
            Only the owner can view their draft.

            **Use case**: Load draft data when user wants to continue editing
            """,
        parameters = {
            @Parameter(name = "draftId", description = "Draft ID", required = true, example = "123")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved draft listing"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Draft not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not own this draft")
        }
    )
    public ApiResponse<DraftListingResponse> getDraftById(@PathVariable Long draftId) {
        String userId = extractUserId();
        DraftListingResponse response = listingService.getDraftById(draftId, userId);
        return ApiResponse.<DraftListingResponse>builder().data(response).build();
    }

    @GetMapping("/my-drafts")
    @Operation(
        summary = "Get my draft listings",
        description = """
            Get all draft listings for the authenticated user.
            Returns drafts sorted by last updated time (newest first).

            **Use case**: Display list of drafts for user to continue editing
            """,
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved draft listings")
        }
    )
    public ApiResponse<List<DraftListingResponse>> getMyDrafts() {
        String userId = extractUserId();
        List<DraftListingResponse> response = listingService.getMyDrafts(userId);
        return ApiResponse.<List<DraftListingResponse>>builder().data(response).build();
    }

    @PostMapping("/draft/{draftId}/publish")
    @Operation(
        summary = "Publish draft listing",
        description = """
            Publish a draft listing after validating all required fields.
            The draft data is merged with the request body (request takes precedence).

            **Required fields** (from draft or request):
            - title, description
            - listingType, productType
            - price, priceUnit
            - address
            - categoryId

            **Payment/Quota options** (in request body):
            - useMembershipQuota: true/false
            - benefitIds: [1, 2] (when using quota)
            - durationDays: 10/15/30 (when paying)
            - paymentProvider: VNPAY (when paying)
            """,
        parameters = {
            @Parameter(name = "draftId", description = "Draft ID", required = true, example = "123")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ListingCreationRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Publish with Quota",
                        value = """
                            {
                              "useMembershipQuota": true,
                              "benefitIds": [1]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Publish with Payment",
                        value = """
                            {
                              "vipType": "GOLD",
                              "durationDays": 30,
                              "paymentProvider": "VNPAY"
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully published draft listing"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing required fields")
        }
    )
    public ApiResponse<ListingCreationResponse> publishDraft(
            @PathVariable Long draftId,
            @RequestBody ListingCreationRequest request) {
        String userId = extractUserId();
        ListingCreationResponse response = listingService.publishDraft(draftId, request, userId);
        return ApiResponse.<ListingCreationResponse>builder().data(response).build();
    }

    @DeleteMapping("/draft/{draftId}")
    @Operation(
        summary = "Delete draft listing",
        description = "Delete a draft listing. **Use case**: User wants to discard a draft",
        parameters = {
            @Parameter(name = "draftId", description = "Draft ID", required = true, example = "123")
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully deleted draft listing"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Draft not found")
        }
    )
    public ApiResponse<Void> deleteDraft(@PathVariable Long draftId) {
        String userId = extractUserId();
        listingService.deleteDraft(draftId, userId);
        return ApiResponse.<Void>builder().message("Draft deleted successfully").build();
    }

    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return authentication.getName();
    }
}
