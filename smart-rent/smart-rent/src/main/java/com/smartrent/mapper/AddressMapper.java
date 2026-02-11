package com.smartrent.mapper;

import com.smartrent.dto.response.*;
import com.smartrent.infra.repository.entity.*;


public interface AddressMapper {

    AddressResponse toResponse(Address address);

    LegacyProvinceResponse toLegacyProvinceResponse(LegacyProvince province);
    LegacyDistrictResponse toLegacyDistrictResponse(District district);
    LegacyWardResponse toLegacyWardResponse(LegacyWard ward);

    NewProvinceResponse toNewProvinceResponse(Province province);
    NewWardResponse toNewWardResponse(Ward ward);
    NewFullAddressResponse toNewFullAddressResponse(Province province, Ward ward);

    // Search response mappings
    NewAddressSearchResponse toNewAddressSearchResponse(Ward ward);
    NewAddressSearchResponse toNewAddressSearchResponseFromProvince(Province province);
}