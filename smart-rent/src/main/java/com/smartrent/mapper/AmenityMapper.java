package com.smartrent.mapper;

import com.smartrent.dto.response.AmenityResponse;
import com.smartrent.infra.repository.entity.Amenity;

public interface AmenityMapper {
    AmenityResponse toResponse(Amenity entity);
}