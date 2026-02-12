package com.smartrent.service.moderation;

import com.smartrent.dto.request.ListingStatusChangeRequest;
import com.smartrent.dto.request.ResolveReportRequest;
import com.smartrent.dto.request.ResubmitListingRequest;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.ModerationEventResponse;
import com.smartrent.dto.response.OwnerActionResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Unified moderation lifecycle service for listings.
 * Handles admin review decisions, report-driven owner actions, and owner resubmissions.
 */
public interface ListingModerationService {

    /**
     * Admin moderates a listing: APPROVE, REJECT, or REQUEST_REVISION.
     */
    ListingResponseWithAdmin moderateListing(Long listingId, ListingStatusChangeRequest request, String adminId);

    /**
     * Extends report resolution to optionally create owner actions and change listing visibility.
     */
    void handleReportResolutionOwnerAction(Long reportId, Long listingId, ResolveReportRequest request, String adminId);

    /**
     * Owner resubmits their listing for review after making required changes.
     */
    void resubmitForReview(Long listingId, String userId, ResubmitListingRequest request);

    /**
     * Get the most recent pending owner action for a listing (if any).
     */
    OwnerActionResponse getOwnerPendingAction(Long listingId);

    /**
     * Batch-load pending owner actions for multiple listings (avoids N+1).
     * Returns a map from listingId to OwnerActionResponse (only listings with pending actions are included).
     */
    Map<Long, OwnerActionResponse> getOwnerPendingActions(Collection<Long> listingIds);

    /**
     * Get the full moderation audit trail for a listing.
     */
    List<ModerationEventResponse> getModerationTimeline(Long listingId);
}
