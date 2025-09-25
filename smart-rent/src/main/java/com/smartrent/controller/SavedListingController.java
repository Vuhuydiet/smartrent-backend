package com.smartrent.controller;

import com.smartrent.controller.dto.request.SavedListingRequest;
import com.smartrent.controller.dto.response.SavedListingResponse;
import com.smartrent.service.listing.SavedListingService;
import com.smartrent.controller.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/saved-listings")
@Tag(name = "Saved Listings", description = "CRUD operations for user saved listings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SavedListingController {

    SavedListingService savedListingService;

    @PostMapping
    public ApiResponse<SavedListingResponse> saveListing(@Valid @RequestBody SavedListingRequest request) {
        SavedListingResponse response = savedListingService.saveListing(request);
        return ApiResponse.<SavedListingResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping("/{listingId}")
    public ApiResponse<Void> unsaveListing(@PathVariable Long listingId) {
        savedListingService.unsaveListing(listingId);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping("/my-saved")
    public ApiResponse<List<SavedListingResponse>> getMySavedListings() {
        List<SavedListingResponse> responses = savedListingService.getMySavedListings();
        return ApiResponse.<List<SavedListingResponse>>builder()
                .data(responses)
                .build();
    }

    @GetMapping("/check/{listingId}")
    public ApiResponse<Boolean> isListingSaved(@PathVariable Long listingId) {
        boolean isSaved = savedListingService.isListingSaved(listingId);
        return ApiResponse.<Boolean>builder()
                .data(isSaved)
                .build();
    }

    @GetMapping("/count")
    public ApiResponse<Long> getMySavedListingsCount() {
        long count = savedListingService.getMySavedListingsCount();
        return ApiResponse.<Long>builder()
                .data(count)
                .build();
    }
}
