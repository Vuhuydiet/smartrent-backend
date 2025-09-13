package com.smartrent.service.listing;

import com.smartrent.controller.dto.request.ListingCreationRequest;
import com.smartrent.controller.dto.request.ListingRequest;
import com.smartrent.controller.dto.response.ListingCreationResponse;
import com.smartrent.controller.dto.response.ListingResponse;

import java.util.List;
import java.util.Set;

public interface ListingService {
    ListingCreationResponse createListing(ListingCreationRequest request);
    ListingResponse getListingById(Long id);
    List<ListingResponse> getListingsByIds(Set<Long> ids);
    ListingResponse updateListing(Long id, ListingRequest request);
    void deleteListing(Long id);
}