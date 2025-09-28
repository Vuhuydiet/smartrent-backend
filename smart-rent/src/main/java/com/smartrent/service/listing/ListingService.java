package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponse;

import java.util.List;
import java.util.Set;

public interface ListingService {
    ListingCreationResponse createListing(ListingCreationRequest request);
    ListingResponse getListingById(Long id);
    List<ListingResponse> getListingsByIds(Set<Long> ids);
    List<ListingResponse> getListings(int page, int size);
    ListingResponse updateListing(Long id, ListingRequest request);
    void deleteListing(Long id);
}