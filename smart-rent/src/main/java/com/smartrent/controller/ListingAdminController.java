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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Listings - Admin",
    description = """
        Admin listing management. All endpoints require `X-Admin-Id` header.

        **Endpoints:**
        - `GET /admin/{id}` - Get full listing detail with admin verification info (use after picking a row from the list)
        - `GET /{id}/admin` - Legacy alias for the detail endpoint above; same payload
        - `POST /admin/list` - Paginated admin listing view returning a SLIM summary per row + dashboard statistics

        **Dashboard statistics:** pendingVerification, verified, expired, rejected counts, VIP tier breakdown.

        **Common filter combinations:**
        - By exact listing ID: `{"id": 12345}` (PK lookup — fastest possible filter)
        - Quick search also matches ID: `{"keyword": "12345"}` matches listing #12345 as well as title/address/description text
        - Pending review: `{"verified": false, "isVerify": true}`
        - By moderation status: `{"moderationStatus": "PENDING_REVIEW"}`
        - By VIP tier: `{"vipType": "GOLD"}`
        - Title contains: `{"title": "Tân Bình"}`
        - Owner name/phone: `{"ownerSearch": "0367919024"}`
        - Posted in a date range: `{"postDate": "2026-03-01..2026-03-31"}` (open-ended: `"2026-03-01.."` or `"..2026-03-31"`)
        - Expiring in a date range: `{"expiryDate": "2026-08-01..2026-08-31"}`
        - By type: `{"listingType": "RENT", "productType": "APARTMENT", "verified": true}`
        - Price range: `{"price": "5000000..15000000"}` (open-ended: `"5000000.."` or `"..15000000"`)
        - Area / bedrooms / bathrooms range: `{"area": "30..60", "bedroomsRange": "2..4", "bathroomsRange": "1..3"}`

        **Range format**: all range filters use the `..` separator (`from..to`). Either side may be omitted.
        Applies to: `price`, `area`, `bedroomsRange`, `bathroomsRange`, `roomCapacity`, `priceReductionPercent`, `postDate`, `expiryDate`.
        """
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_CM')")
public class ListingAdminController {

    private final ListingService listingService;

    @GetMapping("/admin/{id}")
    @Operation(
        summary = "Get full listing detail for admin (Admin only)",
        description = """
            Returns the full listing record including description, media, amenities, address,
            owner contact details, moderation context, and admin verification info.

            Use this endpoint after picking a row from `POST /v1/listings/admin/list`
            (which returns a slim summary per row).
            """,
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
    public ApiResponse<ListingResponseWithAdmin> getAdminListingDetail(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Id") String adminId) {
        ListingResponseWithAdmin response = listingService.getListingByIdWithAdmin(id, adminId);
        return ApiResponse.<ListingResponseWithAdmin>builder().data(response).build();
    }

    @GetMapping("/{id}/admin")
    @Operation(
        summary = "Get full listing detail for admin (legacy alias)",
        description = "Identical to `GET /v1/listings/admin/{id}`. Kept for backward compatibility; prefer the new admin-prefixed route in new code.",
        deprecated = true,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true),
            @Parameter(name = "X-Admin-Id", description = "Admin ID from authentication header", required = true)
        }
    )
    public ApiResponse<ListingResponseWithAdmin> getListingByIdWithAdmin(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Id") String adminId) {
        return getAdminListingDetail(id, adminId);
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
                    ),
                    @ExampleObject(
                        name = "Find by exact listing ID",
                        summary = "Exact match on the listing ID (PK lookup)",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "id": 12345
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Search by title",
                        summary = "Case-insensitive substring match on title",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "title": "Tân Bình"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Find listings by owner",
                        summary = "Match owner name, phone, or email (firstName / lastName / contactPhoneNumber / phoneNumber / email)",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "ownerSearch": "0367919024"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Date range filter",
                        summary = "Listings posted in March 2026, expiring in August 2026 (single field with `..` separator)",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "postDate": "2026-03-01..2026-03-31",
                              "expiryDate": "2026-08-01..2026-08-31"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Open-ended date range",
                        summary = "Posted on or after Mar 1 (no upper bound); expiring on or before Aug 31 (no lower bound)",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "postDate": "2026-03-01..",
                              "expiryDate": "..2026-08-31"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Combined admin filter",
                        summary = "Verified APARTMENT rentals posted in April 2026",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "verified": true,
                              "listingType": "RENT",
                              "productType": "APARTMENT",
                              "postDate": "2026-04-01..2026-04-30"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Numeric range filters",
                        summary = "Price 5M–15M VND, 30–60 m², 2–4 bedrooms — all using `..` separator",
                        value = """
                            {
                              "page": 1,
                              "size": 20,
                              "price": "5000000..15000000",
                              "area": "30..60",
                              "bedroomsRange": "2..4"
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
