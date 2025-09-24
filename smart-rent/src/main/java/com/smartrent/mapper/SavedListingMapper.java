package com.smartrent.mapper;

import com.smartrent.controller.dto.request.SavedListingRequest;
import com.smartrent.controller.dto.response.SavedListingResponse;
import com.smartrent.infra.repository.entity.SavedListing;

public interface SavedListingMapper {
    SavedListing toEntity(SavedListingRequest request, String userId);
    SavedListingResponse toResponse(SavedListing entity);
}
