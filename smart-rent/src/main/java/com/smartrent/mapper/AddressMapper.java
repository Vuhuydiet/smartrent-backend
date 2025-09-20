package com.smartrent.mapper;

import com.smartrent.controller.dto.response.*;
import com.smartrent.infra.repository.entity.*;

public interface AddressMapper {
    AddressResponse toResponse(Address address);
    StreetResponse toResponse(Street street);
    WardResponse toResponse(Ward ward);
    DistrictResponse toResponse(District district);
    ProvinceResponse toResponse(Province province);
}
