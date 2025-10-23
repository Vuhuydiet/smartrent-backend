package com.smartrent.service.address;

import com.smartrent.dto.response.NewAddressSearchResponse;
import com.smartrent.dto.response.NewFullAddressResponse;
import com.smartrent.dto.response.NewProvinceResponse;
import com.smartrent.dto.response.NewWardResponse;
import com.smartrent.dto.response.PaginatedResponse;

import java.util.List;

/**
 * Service interface for new administrative structure (34 provinces after 1/7/2025)
 * Provides integration with tinhthanhpho.com API
 */
public interface NewAddressService {

    /**
     * Get all provinces in new structure (34 provinces)
     *
     * @param keyword Search keyword (optional)
     * @param page Page number (default: 1)
     * @param limit Items per page (default: 20)
     * @return Paginated list of provinces
     */
    PaginatedResponse<List<NewProvinceResponse>> getNewProvinces(String keyword, Integer page, Integer limit);

    /**
     * Get wards by province code in new structure
     *
     * @param provinceCode Province code (e.g., "01" for Hà Nội)
     * @param keyword Search keyword (optional)
     * @param page Page number (default: 1)
     * @param limit Items per page (default: 20)
     * @return Paginated list of wards
     */
    PaginatedResponse<List<NewWardResponse>> getWardsByNewProvince(
            String provinceCode,
            String keyword,
            Integer page,
            Integer limit
    );

    /**
     * Get full address information in new structure
     *
     * @param provinceCode Province code
     * @param wardCode Ward code (optional)
     * @return Full address with province and ward details
     */
    NewFullAddressResponse getNewFullAddress(String provinceCode, String wardCode);

    /**
     * Search addresses in new structure
     *
     * @param keyword Search keyword
     * @param page Page number (default: 1)
     * @param limit Items per page (default: 20)
     * @return Paginated search results
     */
    PaginatedResponse<List<NewAddressSearchResponse>> searchNewAddress(
            String keyword,
            Integer page,
            Integer limit
    );
}