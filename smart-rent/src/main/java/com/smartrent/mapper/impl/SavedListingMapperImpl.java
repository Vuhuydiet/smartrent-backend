package com.smartrent.mapper.impl;

import com.smartrent.controller.dto.request.SavedListingRequest;
import com.smartrent.controller.dto.response.SavedListingResponse;
import com.smartrent.infra.repository.entity.SavedListing;
import com.smartrent.infra.repository.entity.SavedListingId;
import com.smartrent.mapper.SavedListingMapper;
import org.springframework.stereotype.Component;

@Component
public class SavedListingMapperImpl implements SavedListingMapper {

    @Override
    public SavedListing toEntity(SavedListingRequest request, String userId) {
        if (request == null || userId == null) {
            return null;
        }

        SavedListingId id = new SavedListingId(userId, request.getListingId());
        
        return SavedListing.builder()
                .id(id)
                .build();
    }

    @Override
    public SavedListingResponse toResponse(SavedListing entity) {
        if (entity == null) {
            return null;
        }

        return SavedListingResponse.builder()
                .userId(entity.getId().getUserId())
                .listingId(entity.getId().getListingId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
