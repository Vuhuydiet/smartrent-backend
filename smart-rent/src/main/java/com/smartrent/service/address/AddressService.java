package com.smartrent.service.address;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.dto.response.*;

import java.math.BigDecimal;
import java.util.List;

public interface AddressService {

    // Province operations
    List<LegacyProvinceResponse> getAllProvinces();
    List<LegacyProvinceResponse> getParentProvinces(); // For dropdown
    LegacyProvinceResponse getProvinceById(Long provinceId);
    List<LegacyProvinceResponse> searchProvinces(String searchTerm);

    // District operations
    List<LegacyDistrictResponse> getDistrictsByProvinceId(Long provinceId);
    LegacyDistrictResponse getDistrictById(Long districtId);
    List<LegacyDistrictResponse> searchDistricts(String searchTerm, Long provinceId);

    // Ward operations
    List<LegacyWardResponse> getWardsByDistrictId(Long districtId);
    LegacyWardResponse getWardById(Long wardId);
    List<LegacyWardResponse> searchWards(String searchTerm, Long districtId);

    // Street operations
    List<LegacyStreetResponse> getStreetsByWardId(Long wardId);
    LegacyStreetResponse getStreetById(Long streetId);
    List<LegacyStreetResponse> searchStreets(String searchTerm, Long wardId);

    // Address operations
    List<AddressResponse> getAddressesByStreetId(Long streetId);
    AddressResponse getAddressById(Long addressId);
    List<AddressResponse> searchAddresses(String searchTerm);
    List<AddressResponse> getNearbyAddresses(BigDecimal latitude, BigDecimal longitude, Double radiusKm);
    AddressResponse createAddress(AddressCreationRequest request);

    // Auto-complete and mapping
    AddressResponse autoCompleteAddress(String fullAddressText);
    List<AddressResponse> suggestAddresses(String partialAddress);

    // Address structure conversion
    AddressConversionResponse convertLegacyToNew(Integer legacyProvinceId, Integer legacyDistrictId, Integer legacyWardId);
    AddressConversionResponse convertNewToLegacy(String newProvinceCode, String newWardCode);
}
