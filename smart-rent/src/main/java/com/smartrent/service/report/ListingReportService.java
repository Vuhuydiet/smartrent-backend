package com.smartrent.service.report;

import com.smartrent.dto.request.ListingReportRequest;
import com.smartrent.dto.request.ResolveReportRequest;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.PageResponse;
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

    // ============ ADMIN METHODS ============

    /**
     * Get all reports with pagination and optional status filter (Admin only)
     */
    PageResponse<ListingReportResponse> getAllReports(String status, int page, int size);

    /**
     * Get a specific report by ID (Admin only)
     */
    ListingReportResponse getReportById(Long reportId);

    /**
     * Resolve or reject a report (Admin only)
     */
    ListingReportResponse resolveReport(Long reportId, ResolveReportRequest request, String adminId);

    /**
     * Get statistics about reports
     */
    ReportStatistics getReportStatistics();

    /**
     * Inner class for report statistics
     */
    class ReportStatistics {
        private final long totalReports;
        private final long pendingReports;
        private final long resolvedReports;
        private final long rejectedReports;

        public ReportStatistics(long totalReports, long pendingReports, long resolvedReports, long rejectedReports) {
            this.totalReports = totalReports;
            this.pendingReports = pendingReports;
            this.resolvedReports = resolvedReports;
            this.rejectedReports = rejectedReports;
        }

        public long getTotalReports() { return totalReports; }
        public long getPendingReports() { return pendingReports; }
        public long getResolvedReports() { return resolvedReports; }
        public long getRejectedReports() { return rejectedReports; }
    }
}

