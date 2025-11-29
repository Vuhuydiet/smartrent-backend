package com.smartrent.mapper;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseForOwner;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.MediaResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Listing;

import java.util.List;

public interface ListingMapper {
    Listing toEntity(ListingCreationRequest req);

    /**
     * Map Listing entity to ListingResponse with user and address objects
     * @param entity Listing entity
     * @param user User information (owner)
     * @param address Full address information
     * @return ListingResponse
     */
    ListingResponse toResponse(Listing entity, UserCreationResponse user, AddressResponse address);

    ListingCreationResponse toCreationResponse(Listing entity);
    ListingResponseWithAdmin toResponseWithAdmin(Listing entity, Admin verifyingAdmin, String verificationStatus, String verificationNotes);

    /**
     * Map Listing entity to ListingResponseForOwner with owner-specific information
     * @param entity Listing entity
     * @param user User information (owner)
     * @param media List of media attached to the listing
     * @param address Full address response
     * @param paymentInfo Payment information if applicable
     * @param statistics Listing statistics
     * @param verificationNotes Verification notes from admin
     * @param rejectionReason Rejection reason if applicable
     * @return ListingResponseForOwner
     */
    ListingResponseForOwner toResponseForOwner(
            Listing entity,
            UserCreationResponse user,
            List<MediaResponse> media,
            AddressResponse address,
            ListingResponseForOwner.PaymentInfo paymentInfo,
            ListingResponseForOwner.ListingStatistics statistics,
            String verificationNotes,
            String rejectionReason
    );
}