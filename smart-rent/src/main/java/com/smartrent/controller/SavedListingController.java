package com.smartrent.controller;

import com.smartrent.dto.request.SavedListingRequest;
import com.smartrent.dto.response.SavedListingResponse;
import com.smartrent.service.listing.SavedListingService;
import com.smartrent.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/saved-listings")
@Tag(name = "Saved Listings", description = "CRUD operations for user saved listings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SavedListingController {

    SavedListingService savedListingService;

    @PostMapping
    @Operation(
        summary = "Save a listing",
        description = "Add a listing to the user's saved/favorite listings",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Listing saved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SavedListingResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Listing not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Listing already saved"
        )
    })
    public ApiResponse<SavedListingResponse> saveListing(@Valid @RequestBody SavedListingRequest request) {
        SavedListingResponse response = savedListingService.saveListing(request);
        return ApiResponse.<SavedListingResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping("/{listingId}")
    @Operation(
        summary = "Remove a saved listing",
        description = "Remove a listing from the user's saved/favorite listings",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Listing removed from saved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Saved listing not found"
        )
    })
    public ApiResponse<Void> unsaveListing(
            @Parameter(description = "ID of the listing to remove from saved", required = true)
            @PathVariable Long listingId) {
        savedListingService.unsaveListing(listingId);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping("/my-saved")
    @Operation(
        summary = "Get my saved listings",
        description = "Retrieve all listings saved by the current user",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Saved listings retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                    schema = @Schema(implementation = SavedListingResponse.class)
                )
            )
        )
    })
    public ApiResponse<List<SavedListingResponse>> getMySavedListings() {
        List<SavedListingResponse> responses = savedListingService.getMySavedListings();
        return ApiResponse.<List<SavedListingResponse>>builder()
                .data(responses)
                .build();
    }

    @GetMapping("/check/{listingId}")
    @Operation(
        summary = "Check if listing is saved",
        description = "Check whether a specific listing is in the user's saved listings",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Check completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Boolean.class)
            )
        )
    })
    public ApiResponse<Boolean> isListingSaved(
            @Parameter(description = "ID of the listing to check", required = true)
            @PathVariable Long listingId) {
        boolean isSaved = savedListingService.isListingSaved(listingId);
        return ApiResponse.<Boolean>builder()
                .data(isSaved)
                .build();
    }

    @GetMapping("/count")
    @Operation(
        summary = "Get saved listings count",
        description = "Get the total number of listings saved by the current user",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Count retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Long.class)
            )
        )
    })
    public ApiResponse<Long> getMySavedListingsCount() {
        long count = savedListingService.getMySavedListingsCount();
        return ApiResponse.<Long>builder()
                .data(count)
                .build();
    }
}
