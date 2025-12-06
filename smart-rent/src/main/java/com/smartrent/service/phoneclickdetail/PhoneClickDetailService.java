package com.smartrent.service.phoneclickdetail;

import com.smartrent.dto.request.PhoneClickRequest;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.PhoneClickResponse;
import com.smartrent.dto.response.PhoneClickStatsResponse;
import com.smartrent.dto.response.UserPhoneClickDetailResponse;

/**
 * Service for tracking and managing phone number clicks on listings
 */
public interface PhoneClickDetailService {

    /**
     * Track a phone number click on a listing
     *
     * @param request Phone click request containing listing ID
     * @param userId User ID who clicked (from JWT token)
     * @param ipAddress IP address of the request
     * @param userAgent User agent string
     * @return Phone click response
     */
    PhoneClickResponse trackPhoneClick(PhoneClickRequest request, String userId, String ipAddress, String userAgent);

    /**
     * Get all users who clicked on a specific listing's phone number (paginated)
     *
     * @param listingId Listing ID
     * @param page Page number (1-indexed)
     * @param size Page size
     * @return Paginated phone click responses with user details
     */
    PageResponse<PhoneClickResponse> getPhoneClicksByListing(Long listingId, int page, int size);

    /**
     * Get all listings a user has clicked phone numbers on (paginated)
     *
     * @param userId User ID
     * @param page Page number (1-indexed)
     * @param size Page size
     * @return Paginated phone click responses
     */
    PageResponse<PhoneClickResponse> getPhoneClicksByUser(String userId, int page, int size);

    /**
     * Get phone click statistics for a listing
     *
     * @param listingId Listing ID
     * @return Phone click statistics
     */
    PhoneClickStatsResponse getPhoneClickStats(Long listingId);

    /**
     * Get all phone clicks for listings owned by a specific user (renter) (paginated)
     * This is used in the renter's listing management page
     *
     * @param ownerId Owner/renter user ID
     * @param page Page number (1-indexed)
     * @param size Page size
     * @return Paginated phone click responses
     */
    PageResponse<PhoneClickResponse> getPhoneClicksForOwnerListings(String ownerId, int page, int size);

    /**
     * Search phone clicks for listings owned by a specific user by listing title (paginated)
     * This allows renters to search for users who clicked on their listings by listing title
     *
     * @param ownerId Owner/renter user ID
     * @param titleKeyword Listing title keyword to search for
     * @param page Page number (1-indexed)
     * @param size Page size
     * @return Paginated phone click responses
     */
    PageResponse<PhoneClickResponse> searchPhoneClicksByListingTitle(String ownerId, String titleKeyword, int page, int size);

    /**
     * Get user details with their clicked listings for a specific listing (paginated)
     * Each user detail contains a list of listings they have clicked on phone numbers
     *
     * @param listingId Listing ID
     * @param page Page number (1-indexed)
     * @param size Page size
     * @return Paginated user phone click detail responses
     */
    PageResponse<UserPhoneClickDetailResponse> getUsersWithClickedListings(Long listingId, int page, int size);

    /**
     * Get all users who clicked on any of the owner's listings (paginated)
     * Each user detail contains a list of the owner's listings they have clicked on
     * This is used in the renter's listing management page to see all interested users
     *
     * @param ownerId Owner/renter user ID
     * @param page Page number (1-indexed)
     * @param size Page size
     * @return Paginated user phone click detail responses
     */
    PageResponse<UserPhoneClickDetailResponse> getUsersWhoClickedOnMyListings(String ownerId, int page, int size);
}

