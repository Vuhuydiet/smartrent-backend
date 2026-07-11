package com.smartrent.infra.repository;

/**
 * Aggregated report counts for a listing author (người đăng tin).
 * Produced by {@link ListingReportRepository#findReportedAuthors}.
 */
public interface ReportedAuthorProjection {

    /** The listing author's user id. */
    String getUserId();

    /** Total number of reports across all of this author's listings. */
    Long getTotalReports();

    /** Number of reports admins have approved (RESOLVED) across this author's listings. */
    Long getResolvedReports();
}
