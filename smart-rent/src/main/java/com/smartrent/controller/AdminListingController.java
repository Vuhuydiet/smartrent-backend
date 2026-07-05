package com.smartrent.controller;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.response.*;
import com.smartrent.service.listing.ListingService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/listings")
@Tag(
    name = "Listings - Admin",
    description = """
        Admin listing management. All endpoints require the `X-Admin-Id` header.

        **Endpoints:**
        - `GET /v1/admin/listings` - Paginated admin listing view (slim summary per row + dashboard statistics).
          Filters are plain query parameters mirroring `ListingFilterRequest` fields (e.g. `?verified=false&isVerify=true`).
        - `GET /v1/admin/listings/{id}` - Full listing detail with admin verification info (use after picking a row from the list).

        **Dashboard statistics:** pendingVerification, verified, expired, rejected counts, VIP tier breakdown.

        **Common query examples:**
        - Pending review: `?verified=false&isVerify=true`
        - By moderation status: `?moderationStatus=PENDING_REVIEW`
        - By VIP tier: `?vipType=GOLD`
        - Title contains: `?title=T%C3%A2n%20B%C3%ACnh`
        - Owner name/phone: `?ownerSearch=0367919024`
        - Posted in a date range: `?postDate=2026-03-01..2026-03-31` (open-ended: `2026-03-01..` or `..2026-03-31`)
        - Expiring in a date range: `?expiryDate=2026-08-01..2026-08-31`
        - By type: `?listingType=RENT&productType=APARTMENT&verified=true`
        - Price range: `?price=5000000..15000000` (open-ended: `5000000..` or `..15000000`)
        - Area / bedrooms / bathrooms range: `?area=30..60&bedroomsRange=2..4&bathroomsRange=1..3`

        **Range format**: all range filters use the `..` separator (`from..to`). Either side may be omitted.
        Applies to: `price`, `area`, `bedroomsRange`, `bathroomsRange`, `roomCapacity`, `priceReductionPercent`, `postDate`, `expiryDate`.
        """
)
@RequiredArgsConstructor
public class AdminListingController {

    private final ListingService listingService;

    @GetMapping
    public ApiResponse<AdminListingListResponse> getAllListingsForAdmin(
            @RequestHeader("X-Admin-Id") String adminId,
            @Valid ListingFilterRequest filter) {
        filter.setIsAdminRequest(true);
        AdminListingListResponse response = listingService.getAllListingsForAdmin(filter, adminId);
        return ApiResponse.<AdminListingListResponse>builder().data(response).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ListingResponseWithAdmin> getAdminListingDetail(
            @Parameter(description = "Listing ID", required = true) @PathVariable Long id,
            @Parameter(description = "Admin ID from authentication header", required = true) @RequestHeader("X-Admin-Id") String adminId) {
        ListingResponseWithAdmin response = listingService.getListingByIdWithAdmin(id, adminId);
        return ApiResponse.<ListingResponseWithAdmin>builder().data(response).build();
    }
}
