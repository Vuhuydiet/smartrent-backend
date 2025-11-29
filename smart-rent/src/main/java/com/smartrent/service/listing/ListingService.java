package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.MyListingsFilterRequest;
import com.smartrent.dto.request.ProvinceStatsRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
import com.smartrent.dto.response.AdminListingListResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingListResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseForOwner;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.OwnerListingListResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.dto.response.ProvinceListingStatsResponse;

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

    /**
     * Get listing statistics by provinces (for home screen)
     * Returns statistics for each province including total listings, verified, and VIP counts
     * @param request Province statistics request (provinceIds or provinceCodes)
     * @return List of province statistics
     */
    List<ProvinceListingStatsResponse> getProvinceStats(ProvinceStatsRequest request);

    /**
     * Get my listing detail with owner-specific information (Owner only)
     * Returns detailed listing information including transaction details, media, payment info, etc.
     * Only the owner can view this information.
     *
     * @param id Listing ID
     * @param userId User ID (owner) who is requesting the listing details
     * @return ListingResponseForOwner containing listing details with owner-specific information
     * @throws RuntimeException if listing not found or user is not the owner
     */
    ListingResponseForOwner getMyListingDetail(Long id, String userId);

    /**
     * Get all listings for admin with pagination and filters (Admin only)
     * Returns paginated list of all listings with admin-specific information
     * Supports comprehensive filtering and statistics
     *
     * @param filter Filter criteria (all fields optional) - supports all listing filters
     * @param adminId Admin ID who is requesting the listing list
     * @return AdminListingListResponse containing paginated listings with admin info and statistics
     */
    AdminListingListResponse getAllListingsForAdmin(ListingFilterRequest filter, String adminId);

    /**
     * Get my listings with owner-specific information (Owner only)
     * Returns paginated list of owner's listings with detailed information including
     * transaction details, media, payment info, and statistics for each listing
     *
     * @param filter Filter criteria (verified, expired, isDraft, vipType, etc.)
     * @param userId User ID (owner) who is requesting the listings
     * @return OwnerListingListResponse containing paginated listings with owner-specific information and statistics
     */
    OwnerListingListResponse getMyListings(ListingFilterRequest filter, String userId);
}