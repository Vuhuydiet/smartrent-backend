package com.smartrent.service.address;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.infra.repository.entity.Address;

/**
 * Service for creating and managing addresses
 */
public interface AddressCreationService {

    /**
     * Create a new address from request
     * Builds formatted address strings and stores metadata for querying
     *
     * @param request Address creation request with all required fields
     * @return Created address entity
     */
    Address createAddress(AddressCreationRequest request);

    /**
     * Build formatted address string for old structure (63 provinces, 3-tier)
     *
     * @param request Address creation request
     * @return Formatted address string
     */
    String buildOldAddressString(AddressCreationRequest request);

    /**
     * Build formatted address string for new structure (34 provinces, 2-tier)
     *
     * @param request Address creation request
     * @return Formatted address string
     */
    String buildNewAddressString(AddressCreationRequest request);

    /**
     * Validate address request fields
     *
     * @param request Address creation request
     * @throws IllegalArgumentException if validation fails
     */
    void validateAddressRequest(AddressCreationRequest request);
}
