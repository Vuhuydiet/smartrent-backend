package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.MyListingsFilterRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingListResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseWithAdmin;
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

    /**
     * Get listing by ID with admin verification information (Admin only)
     * @param id Listing ID
     * @param adminId Admin ID who is requesting the listing details
     * @return ListingResponseWithAdmin containing listing details and admin verification info
     */
    ListingResponseWithAdmin getListingByIdWithAdmin(Long id, String adminId);

    /**
     * Complete listing creation (both NORMAL and VIP) after successful payment
     * This unified method handles both NORMAL and VIP listings by checking cache
     * @param transactionId Transaction ID
     * @return Listing creation response
     */
    ListingCreationResponse completeListingCreationAfterPayment(String transactionId);

    /**
     * Unified search API - handles both public search and my listings
     * @param filter Filter criteria (all fields optional)
     *               - If userId is null: public search (excludes drafts by default)
     *               - If userId is provided: user's listings (includes drafts if isDraft filter allows)
     * @return Paginated listing response with total count and recommendations
     */
    ListingListResponse searchListings(ListingFilterRequest filter);
}