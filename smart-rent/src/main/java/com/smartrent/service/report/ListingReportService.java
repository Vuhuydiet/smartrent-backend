package com.smartrent.service.report;

import com.smartrent.dto.request.ListingReportRequest;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.ReportReasonResponse;

import java.util.List;

public interface ListingReportService {

    /**
     * Get all active report reasons
     */
    List<ReportReasonResponse> getReportReasons();

    /**
     * Create a new report for a listing
     */
    ListingReportResponse createReport(Long listingId, ListingReportRequest request);

    /**
     * Get report history for a specific listing
     */
    List<ListingReportResponse> getReportHistory(Long listingId);

    /**
     * Get report count for a specific listing
     */
    long getReportCount(Long listingId);
}

