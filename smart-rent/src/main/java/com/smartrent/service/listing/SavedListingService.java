package com.smartrent.service.listing;

import com.smartrent.dto.request.SavedListingRequest;
import com.smartrent.dto.response.SavedListingResponse;

import java.util.List;

public interface SavedListingService {
    SavedListingResponse saveListing(SavedListingRequest request);
    void unsaveListing(Long listingId);
    List<SavedListingResponse> getMySavedListings();
    boolean isListingSaved(Long listingId);
    long getMySavedListingsCount();
}
