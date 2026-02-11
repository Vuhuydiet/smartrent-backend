package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingStatusChangeRequest;
import com.smartrent.dto.response.ListingResponseWithAdmin;

public interface VerifyListingService {

    /**
     * Changes the verification status of a listing
     * Only admin users can perform this operation
     *
     * @param listingId The ID of the listing to update
     * @param request The status change request containing the new verification status
     * @return Updated listing information with admin verification details
     */
    ListingResponseWithAdmin changeListingStatus(Long listingId, ListingStatusChangeRequest request);
}
