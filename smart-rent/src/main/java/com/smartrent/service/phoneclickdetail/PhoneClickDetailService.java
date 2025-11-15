package com.smartrent.service.phoneclickdetail;

import com.smartrent.dto.request.PhoneClickRequest;
import com.smartrent.dto.response.PhoneClickResponse;
import com.smartrent.dto.response.PhoneClickStatsResponse;

import java.util.List;

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
     * Get all users who clicked on a specific listing's phone number
     * 
     * @param listingId Listing ID
     * @return List of phone click responses with user details
     */
    List<PhoneClickResponse> getPhoneClicksByListing(Long listingId);

    /**
     * Get all listings a user has clicked phone numbers on
     * 
     * @param userId User ID
     * @return List of phone click responses
     */
    List<PhoneClickResponse> getPhoneClicksByUser(String userId);

    /**
     * Get phone click statistics for a listing
     * 
     * @param listingId Listing ID
     * @return Phone click statistics
     */
    PhoneClickStatsResponse getPhoneClickStats(Long listingId);

    /**
     * Get all phone clicks for listings owned by a specific user (renter)
     * This is used in the renter's listing management page
     * 
     * @param ownerId Owner/renter user ID
     * @return List of phone click responses
     */
    List<PhoneClickResponse> getPhoneClicksForOwnerListings(String ownerId);
}

