package com.smartrent.mapper;

import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.infra.repository.entity.Listing;

/**
 * Mapper interface for converting Listing entities to AI verification requests
 */
public interface AiListingMapper {
    
    /**
     * Convert a Listing entity to an AI verification request
     * 
     * @param listing The listing entity to convert
     * @return AI verification request with listing data
     */
    AiListingVerificationRequest toVerificationRequest(Listing listing);
}