package com.smartrent.mapper.impl;

import com.smartrent.dto.response.AmenityResponse;
import com.smartrent.infra.repository.entity.Amenity;
import com.smartrent.mapper.AmenityMapper;
import org.springframework.stereotype.Component;

@Component
public class AmenityMapperImpl implements AmenityMapper {

    @Override
    public AmenityResponse toResponse(Amenity entity) {
        if (entity == null) {
            return null;
        }

        return AmenityResponse.builder()
                .amenityId(entity.getAmenityId())
                .name(entity.getName())
                .icon(entity.getIcon())
                .description(entity.getDescription())
                .category(entity.getCategory() != null ? entity.getCategory().name() : null)
                .isActive(entity.getIsActive())
                .build();
    }
}