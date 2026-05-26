package com.smartrent.mapper;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.AdminListingSummary;
import com.smartrent.dto.response.ListingCardResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseForOwner;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.MediaResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.User;

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

    /**
     * Map Listing entity to the minimal ListingCardResponse for public card display.
     * Skips amenities, contact fields, and all detail-only fields.
     */
    ListingCardResponse toCardResponse(Listing entity, UserCreationResponse user, AddressResponse address);

    ListingCreationResponse toCreationResponse(Listing entity);

    /**
     * Map Listing entity to ListingResponseWithAdmin with admin verification info and user details
     * @param entity Listing entity
     * @param user User information (owner)
     * @param verifyingAdmin Admin who verified/updated the listing (can be null)
     * @param verificationStatus Verification status (PENDING/APPROVED/REJECTED)
     * @param verificationNotes Verification notes from admin
     * @return ListingResponseWithAdmin
     */
    ListingResponseWithAdmin toResponseWithAdmin(Listing entity, UserCreationResponse user, Admin verifyingAdmin, String verificationStatus, String verificationNotes);

    /**
     * Map Listing entity to slim AdminListingSummary used by the admin list/table view.
     * Drops description, amenities, address/extra-fee fields, etc. — call
     * GET /v1/listings/admin/{id} for the full record.
     *
     * @param entity Listing entity
     * @param owner Listing owner entity (nullable). Used to build the slim OwnerSummary.
     * @param verificationStatus Verification status (PENDING/APPROVED/REJECTED/NOT_SUBMITTED)
     * @param images Pre-resolved image URLs for the listing (primary first, then sortOrder).
     */
    AdminListingSummary toAdminSummary(Listing entity, User owner, String verificationStatus, List<String> images);

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