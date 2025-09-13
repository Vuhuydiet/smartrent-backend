package com.smartrent.controller;

import com.smartrent.controller.dto.request.ListingCreationRequest;
import com.smartrent.controller.dto.request.ListingRequest;
import com.smartrent.controller.dto.response.ListingCreationResponse;
import com.smartrent.controller.dto.response.ListingResponse;
import com.smartrent.service.listing.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    public ResponseEntity<ListingCreationResponse> createListing(@Valid @RequestBody ListingCreationRequest request) {
        ListingCreationResponse response = listingService.createListing(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        ListingResponse response = listingService.getListingById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ListingResponse>> getListingsByIds(@RequestParam("ids") Set<Long> ids) {
        List<ListingResponse> responses = listingService.getListingsByIds(ids);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> updateListing(@PathVariable Long id, @RequestBody ListingRequest request) {
        ListingResponse response = listingService.updateListing(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        listingService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }
}