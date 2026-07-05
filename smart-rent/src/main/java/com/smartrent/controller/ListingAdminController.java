package com.smartrent.controller;

import com.smartrent.dto.response.*;
import com.smartrent.service.listing.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Listings - Admin",
    description = """
        Legacy admin listing detail alias. Requires the `X-Admin-Id` header.

        Prefer `GET /v1/admin/listings/{id}` (see `AdminListingController`) and
        `GET /v1/admin/listings` for new integrations — this endpoint is kept only
        for backward compatibility with existing callers.
        """
)
@RequiredArgsConstructor
public class ListingAdminController {

    private final ListingService listingService;

    @GetMapping("/{id}/admin")
    @Operation(
        summary = "Get full listing detail for admin (legacy alias)",
        description = "Identical to `GET /v1/admin/listings/{id}`. Kept for backward compatibility; prefer the new route in new code.",
        deprecated = true,
        parameters = {
            @Parameter(name = "id", description = "Listing ID", required = true),
            @Parameter(name = "X-Admin-Id", description = "Admin ID from authentication header", required = true)
        }
    )
    public ApiResponse<ListingResponseWithAdmin> getListingByIdWithAdmin(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Id") String adminId) {
        ListingResponseWithAdmin response = listingService.getListingByIdWithAdmin(id, adminId);
        return ApiResponse.<ListingResponseWithAdmin>builder().data(response).build();
    }
}
