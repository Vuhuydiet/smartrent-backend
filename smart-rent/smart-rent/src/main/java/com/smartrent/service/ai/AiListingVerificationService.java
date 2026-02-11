package com.smartrent.service.ai;

import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
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
     * Check if the AI verification service is available
     * 
     * @return true if the service is available, false otherwise
     */
    boolean isServiceAvailable();
}