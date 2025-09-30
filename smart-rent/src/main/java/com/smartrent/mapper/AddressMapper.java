package com.smartrent.mapper;

import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.DistrictResponse;
import com.smartrent.dto.response.ProvinceResponse;
import com.smartrent.dto.response.StreetResponse;
import com.smartrent.dto.response.WardResponse;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.Province;
import com.smartrent.infra.repository.entity.Street;
import com.smartrent.infra.repository.entity.Ward;

public interface AddressMapper {
    AddressResponse toResponse(Address address);
    StreetResponse toResponse(Street street);
    WardResponse toResponse(Ward ward);
    DistrictResponse toResponse(District district);
    ProvinceResponse toResponse(Province province);
}
