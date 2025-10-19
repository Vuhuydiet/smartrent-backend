package com.smartrent.service.address;

import com.smartrent.dto.request.BatchAddressConversionRequest;
import com.smartrent.dto.response.*;

import java.util.List;

/**
 * Enhanced Address Service with code-based operations and conversion features
 * This service handles both old and new administrative structures
 */
public interface EnhancedAddressService {

    // ========================================================================
    // Code-based Operations (RESTful approach)
    // ========================================================================

    /**
     * Get all provinces (current/active)
     */
    List<ProvinceResponse> getAllProvinces();

    /**
     * Get districts by province code
     */
    List<DistrictResponse> getDistrictsByProvinceCode(String provinceCode);

    /**
     * Get wards by district code
     */
    List<WardResponse> getWardsByDistrictCode(String districtCode);

    // ========================================================================
    // Full Address Operations
    // ========================================================================

    /**
     * Build full address from codes (province, district, ward)
     * @param provinceCode Province code
     * @param districtCode District code
     * @param wardCode Ward code
     * @return Complete address hierarchy
     */
    FullAddressResponse getFullAddress(String provinceCode, String districtCode, String wardCode);

    // ========================================================================
    // New Administrative Structure Operations
    // ========================================================================

    /**
     * Get all provinces using new administrative structure
     * (Shows merged provinces as separate entries with references to parent)
     */
    List<ProvinceResponse> getNewProvinces();

    /**
     * Get wards directly by province code (flattened structure)
     * Useful for "new" administrative approach where districts might be optional
     */
    List<WardResponse> getWardsByProvinceCode(String provinceCode);

    /**
     * Build full address using new administrative codes
     */
    FullAddressResponse getNewFullAddress(String provinceCode, String districtCode, String wardCode);

    // ========================================================================
    // Search Operations
    // ========================================================================

    /**
     * Search addresses using old/current administrative structure
     * @param query Search query (can include province, district, ward names)
     * @param limit Maximum results to return
     * @return Search results with relevance scores
     */
    AddressSearchResponse searchAddress(String query, Integer limit);

    /**
     * Search addresses using new administrative structure
     * (includes merged entities)
     */
    AddressSearchResponse searchNewAddress(String query, Integer limit);

    // ========================================================================
    // Address Conversion Operations
    // ========================================================================

    /**
     * Convert single address from old to new administrative structure
     * Handles province/ward mergers and reorganizations
     */
    AddressConversionResponse convertAddress(String provinceCode, String districtCode, String wardCode);

    /**
     * Batch convert multiple addresses
     * @param request Batch of addresses to convert
     * @return List of conversion results
     */
    List<AddressConversionResponse> convertAddressesBatch(BatchAddressConversionRequest request);

    // ========================================================================
    // Merge History Operations
    // ========================================================================

    /**
     * Get province merge history
     * Shows if province was merged, when, and into what
     */
    MergeHistoryResponse getProvinceMergeHistory(String provinceCode);

    /**
     * Get ward merge history
     */
    MergeHistoryResponse getWardMergeHistory(String wardCode);

    // ========================================================================
    // Utility Operations
    // ========================================================================

    /**
     * Validate if address codes are valid and active
     */
    boolean validateAddressCodes(String provinceCode, String districtCode, String wardCode);

    /**
     * Get effective date for administrative change
     */
    java.time.LocalDate getEffectiveDate(String entityCode, String entityType);
}
