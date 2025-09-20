package com.smartrent.service.address;

import com.smartrent.controller.dto.request.AddressCreationRequest;
import com.smartrent.controller.dto.response.*;

import java.math.BigDecimal;
import java.util.List;

public interface AddressService {

    // Province operations
    List<ProvinceResponse> getAllProvinces();
    List<ProvinceResponse> getParentProvinces(); // For dropdown
    ProvinceResponse getProvinceById(Long provinceId);
    List<ProvinceResponse> searchProvinces(String searchTerm);

    // District operations
    List<DistrictResponse> getDistrictsByProvinceId(Long provinceId);
    DistrictResponse getDistrictById(Long districtId);
    List<DistrictResponse> searchDistricts(String searchTerm, Long provinceId);

    // Ward operations
    List<WardResponse> getWardsByDistrictId(Long districtId);
    WardResponse getWardById(Long wardId);
    List<WardResponse> searchWards(String searchTerm, Long districtId);

    // Street operations
    List<StreetResponse> getStreetsByWardId(Long wardId);
    StreetResponse getStreetById(Long streetId);
    List<StreetResponse> searchStreets(String searchTerm, Long wardId);

    // Address operations
    List<AddressResponse> getAddressesByStreetId(Long streetId);
    AddressResponse getAddressById(Long addressId);
    List<AddressResponse> searchAddresses(String searchTerm);
    List<AddressResponse> getNearbyAddresses(BigDecimal latitude, BigDecimal longitude, Double radiusKm);
    AddressResponse createAddress(AddressCreationRequest request);

    // Auto-complete and mapping
    AddressResponse autoCompleteAddress(String fullAddressText);
    List<AddressResponse> suggestAddresses(String partialAddress);
}
