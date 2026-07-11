package com.smartrent.service.report;

import com.smartrent.dto.request.PostingBlockRequest;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.ReportedAuthorResponse;

import java.util.List;

/**
 * Admin service for tracking listing authors (người đăng tin) by their report history
 * and for blocking / unblocking them from posting new listings.
 */
public interface ReportedAuthorService {

    /** Number of admin-approved (RESOLVED) reports beyond which an author becomes block-eligible. */
    int BLOCK_ELIGIBLE_THRESHOLD = 3;

    /**
     * Paginated list of authors who have at least one report on their listings,
     * with total / approved report counts and current block state.
     */
    PageResponse<ReportedAuthorResponse> getReportedAuthors(int page, int size);

    /**
     * All admin-approved (RESOLVED) reports across a given author's listings.
     */
    List<ListingReportResponse> getApprovedReports(String userId);

    /**
     * Block or unblock an author from posting new listings.
     */
    ReportedAuthorResponse setPostingBlock(String userId, PostingBlockRequest request, String adminId);
}
