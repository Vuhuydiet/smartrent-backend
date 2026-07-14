package com.smartrent.service.ai;

import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.dto.response.DuplicateCheckResponse;
import com.smartrent.infra.repository.entity.Listing;

/**
 * Service interface for AI-powered listing verification.
 * Integrates with external AI service to validate listing quality and compliance.
 */
public interface AiListingVerificationService {

    /**
     * Verify a listing using AI analysis
     * 
     * @param request The verification request containing listing details
     * @return Verification results with scores, issues, and suggestions
     */
    AiListingVerificationResponse verifyListing(AiListingVerificationRequest request);

    /**
     * Verify a listing entity by converting it to a verification request
     * 
     * @param listing The listing entity to verify
     * @return Verification results with scores, issues, and suggestions
     */
    AiListingVerificationResponse verifyListing(Listing listing);

    /**
     * Build verification request from a listing ID (handles lazy loading)
     *
     * @param listingId The listing ID
     * @return The verification request DTO
     */
    AiListingVerificationRequest buildVerificationRequest(Long listingId);

    /**
     * Run a duplicate-listing check for an existing listing by ID. Builds the
     * duplicate-check request from the stored listing (title/description/price/
     * area/images + resolved location + product type) and calls the AI service.
     * Stateless — returns the structured duplicate result for the admin to use
     * during manual review; persists nothing.
     *
     * @param listingId The listing ID
     * @return The duplicate-check result (decision, highest score, matches)
     */
    DuplicateCheckResponse checkDuplicateById(Long listingId);

    /**
     * Check if the AI verification service is available
     *
     * @return true if the service is available, false otherwise
     */
    boolean isServiceAvailable();
}