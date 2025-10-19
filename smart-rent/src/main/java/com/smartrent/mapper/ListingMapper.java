package com.smartrent.mapper;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Listing;

public interface ListingMapper {
    Listing toEntity(ListingCreationRequest req);
    ListingResponse toResponse(Listing entity);
    ListingCreationResponse toCreationResponse(Listing entity);
    ListingResponseWithAdmin toResponseWithAdmin(Listing entity, Admin verifyingAdmin, String verificationStatus, String verificationNotes);
}