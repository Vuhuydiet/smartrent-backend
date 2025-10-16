package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.PaymentResponse;

import java.util.List;
import java.util.Set;

public interface ListingService {
    ListingCreationResponse createListing(ListingCreationRequest request);

    /**
     * Create VIP or Premium listing with quota check and payment flow
     * If user has quota, uses quota and creates listing immediately
     * If no quota, creates transaction and returns payment URL
     */
    Object createVipListing(VipListingCreationRequest request);

    /**
     * Complete VIP listing creation after successful payment
     * Called from payment callback handler
     */
    ListingCreationResponse completeVipListingCreation(String transactionId);

    ListingResponse getListingById(Long id);
    List<ListingResponse> getListingsByIds(Set<Long> ids);
    List<ListingResponse> getListings(int page, int size);
    ListingResponse updateListing(Long id, ListingRequest request);
    void deleteListing(Long id);
}