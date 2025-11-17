package com.smartrent.service.address;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.dto.response.*;

import java.math.BigDecimal;
import java.util.List;

public interface AddressService {

    // Province operations
    List<LegacyProvinceResponse> getAllProvinces();
    List<LegacyProvinceResponse> getParentProvinces(); // For dropdown
    LegacyProvinceResponse getProvinceById(Integer provinceId);
    List<LegacyProvinceResponse> searchProvinces(String searchTerm);

    // District operations
    List<LegacyDistrictResponse> getDistrictsByProvinceId(Integer provinceId);
    LegacyDistrictResponse getDistrictById(Integer districtId);
    List<LegacyDistrictResponse> searchDistricts(String searchTerm, Integer provinceId);

    // Ward operations
    List<LegacyWardResponse> getWardsByDistrictId(Integer districtId);
    LegacyWardResponse getWardById(Integer wardId);
    List<LegacyWardResponse> searchWards(String searchTerm, Integer districtId);

    // Address operations
    AddressResponse getAddressById(Integer addressId);
    List<AddressResponse> searchAddresses(String searchTerm);
    List<AddressResponse> getNearbyAddresses(BigDecimal latitude, BigDecimal longitude, Double radiusKm);
    AddressResponse createAddress(AddressCreationRequest request);

    // Auto-complete and mapping
    AddressResponse autoCompleteAddress(String fullAddressText);
    List<AddressResponse> suggestAddresses(String partialAddress);

    // Address structure conversion
    AddressConversionResponse convertLegacyToNew(Integer legacyProvinceId, Integer legacyDistrictId, Integer legacyWardId);

    // Merge history
    AddressMergeHistoryResponse getMergeHistory(String newProvinceCode, String newWardCode);
}
