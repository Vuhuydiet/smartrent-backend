package com.smartrent.controller;

import com.smartrent.controller.dto.request.SavedListingRequest;
import com.smartrent.controller.dto.response.SavedListingResponse;
import com.smartrent.service.listing.SavedListingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/saved-listings")
@Tag(name = "Saved Listings", description = "CRUD operations for user saved listings")
@RequiredArgsConstructor
public class SavedListingController {

    private final SavedListingService savedListingService;

    @Operation(
        summary = "Save a listing for the authenticated user",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SavedListingRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing saved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SavedListingResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Listing is already saved by this user"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            )
        }
    )
    @PostMapping
    public ResponseEntity<SavedListingResponse> saveListing(@Valid @RequestBody SavedListingRequest request) {
        SavedListingResponse response = savedListingService.saveListing(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Remove a saved listing for the authenticated user",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        parameters = {
            @Parameter(name = "listingId", description = "Listing ID", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "Saved listing removed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Saved listing not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            )
        }
    )
    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> unsaveListing(@PathVariable Long listingId) {
        savedListingService.unsaveListing(listingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Get all saved listings for the authenticated user",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "List of saved listings",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SavedListingResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            )
        }
    )
    @GetMapping("/my-saved")
    public ResponseEntity<List<SavedListingResponse>> getMySavedListings() {
        List<SavedListingResponse> responses = savedListingService.getMySavedListings();
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Check if a listing is saved by the authenticated user",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        parameters = {
            @Parameter(name = "listingId", description = "Listing ID", required = true)
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Returns true if listing is saved, false otherwise",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "boolean")
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            )
        }
    )
    @GetMapping("/check/{listingId}")
    public ResponseEntity<Boolean> isListingSaved(@PathVariable Long listingId) {
        boolean isSaved = savedListingService.isListingSaved(listingId);
        return ResponseEntity.ok(isSaved);
    }

    @Operation(
        summary = "Get count of saved listings for the authenticated user",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Count of saved listings",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64")
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            )
        }
    )
    @GetMapping("/count")
    public ResponseEntity<Long> getMySavedListingsCount() {
        long count = savedListingService.getMySavedListingsCount();
        return ResponseEntity.ok(count);
    }
}
