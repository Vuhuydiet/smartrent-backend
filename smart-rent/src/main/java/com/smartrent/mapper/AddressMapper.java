package com.smartrent.mapper;

import com.smartrent.dto.response.*;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.Province;
import com.smartrent.infra.repository.entity.Street;
import com.smartrent.infra.repository.entity.Ward;

public interface AddressMapper {
    // Legacy structure mappings
    AddressResponse toResponse(Address address);
    StreetResponse toResponse(Street street);
    WardResponse toResponse(Ward ward);
    DistrictResponse toResponse(District district);
    ProvinceResponse toResponse(Province province);

    // New 2025 structure mappings
    NewProvinceResponse toNewProvinceResponse(Province province);
    NewWardResponse toNewWardResponse(Ward ward);
    NewFullAddressResponse toNewFullAddressResponse(Province province, Ward ward);
    NewAddressSearchResponse toNewAddressSearchResponse(Ward ward);
    NewAddressSearchResponse toNewAddressSearchResponseFromProvince(Province province);
}
