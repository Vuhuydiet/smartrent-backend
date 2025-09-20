package com.smartrent.mapper;

import com.smartrent.controller.dto.request.ListingCreationRequest;
import com.smartrent.controller.dto.response.ListingCreationResponse;
import com.smartrent.controller.dto.response.ListingResponse;
import com.smartrent.infra.repository.entity.Listing;

public interface ListingMapper {
    Listing toEntity(ListingCreationRequest req);
    ListingResponse toResponse(Listing entity);
    ListingCreationResponse toCreationResponse(Listing entity);
}