package com.smartrent.infra.repository;

import com.smartrent.enums.ReportStatus;
import com.smartrent.infra.repository.entity.ListingReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ListingReportRepository extends JpaRepository<ListingReport, Long> {

    /**
     * Find all reports for a specific listing
     */
    List<ListingReport> findByListingIdOrderByCreatedAtDesc(Long listingId);

    /**
     * Find all reports for a specific listing with pagination
     */
    Page<ListingReport> findByListingIdOrderByCreatedAtDesc(Long listingId, Pageable pageable);

    /**
     * Count reports for a specific listing
     */
    long countByListingId(Long listingId);

    /**
     * Find reports by reporter email
     */
    List<ListingReport> findByReporterEmailOrderByCreatedAtDesc(String reporterEmail);

    /**
     * Find reports by reporter phone
     */
    List<ListingReport> findByReporterPhoneOrderByCreatedAtDesc(String reporterPhone);

    /**
     * Check if a user has already reported a listing
     */
    @Query("SELECT COUNT(r) > 0 FROM listing_reports r WHERE r.listingId = :listingId AND (r.reporterEmail = :email OR r.reporterPhone = :phone)")
    boolean existsByListingIdAndReporter(@Param("listingId") Long listingId, @Param("email") String email, @Param("phone") String phone);

    /**
     * Find all reports with pagination (for admin)
     */
    Page<ListingReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find reports by status with pagination
     */
    Page<ListingReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    /**
     * Find reports by status
     */
    List<ListingReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    /**
     * Count reports by status
     */
    long countByStatus(ReportStatus status);

    /**
     * Find reports resolved by a specific admin
     */
    List<ListingReport> findByResolvedByOrderByResolvedAtDesc(String adminId);

    /**
     * Find reports resolved by a specific admin with pagination
     */
    Page<ListingReport> findByResolvedByOrderByResolvedAtDesc(String adminId, Pageable pageable);

    @Query(value = "SELECT DATE(r.created_at) AS label, COUNT(*) AS cnt " +
            "FROM listing_reports r WHERE r.created_at BETWEEN :start AND :end " +
            "GROUP BY DATE(r.created_at) ORDER BY label ASC", nativeQuery = true)
    List<Object[]> countReportsByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT DATE_FORMAT(r.created_at, '%Y-%m') AS label, COUNT(*) AS cnt " +
            "FROM listing_reports r WHERE r.created_at BETWEEN :start AND :end " +
            "GROUP BY DATE_FORMAT(r.created_at, '%Y-%m') ORDER BY label ASC", nativeQuery = true)
    List<Object[]> countReportsByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT r.category AS label, COUNT(*) AS cnt FROM listing_reports r " +
            "WHERE r.created_at BETWEEN :start AND :end " +
            "GROUP BY r.category ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countReportsByCategory(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT r.status AS label, COUNT(*) AS cnt FROM listing_reports r " +
            "WHERE r.created_at BETWEEN :start AND :end " +
            "GROUP BY r.status ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countReportsByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(MINUTE, r.created_at, r.resolved_at)) FROM listing_reports r " +
            "WHERE r.created_at BETWEEN :start AND :end AND r.resolved_at IS NOT NULL", nativeQuery = true)
    Double avgResolutionMinutes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByCreatedAtBefore(LocalDateTime dateTime);

    /**
     * Aggregate reports per listing author (người đăng tin). Each row is one author who
     * has at least one report on any of their listings, with total and admin-approved
     * (RESOLVED) report counts. Ordered by most approved reports first.
     */
    /**
     * Reported authors with optional filters. Empty-string text filters and a
     * {@code blockEligibleFlag} of -1 mean "no filter" (sentinels avoid binding
     * nulls in a native query). {@code blockEligibleFlag}: -1 all, 1 eligible
     * (resolved &gt; threshold), 0 not eligible. Text filters are prefix matches so
     * they can use the users indexes.
     */
    @Query(value = "SELECT l.user_id AS userId, " +
            "CAST(COUNT(*) AS SIGNED) AS totalReports, " +
            "CAST(SUM(CASE WHEN r.status = 'RESOLVED' THEN 1 ELSE 0 END) AS SIGNED) AS resolvedReports " +
            "FROM listing_reports r " +
            "JOIN listings l ON r.listing_id = l.listing_id " +
            "JOIN users u ON u.user_id = l.user_id " +
            "WHERE (:email = '' OR u.email LIKE CONCAT(:email, '%')) " +
            "AND (:name = '' OR u.first_name LIKE CONCAT(:name, '%') OR u.last_name LIKE CONCAT(:name, '%')) " +
            "AND (:phone = '' OR u.phone_number LIKE CONCAT(:phone, '%')) " +
            "GROUP BY l.user_id " +
            "HAVING (:blockEligibleFlag = -1 " +
            "  OR (:blockEligibleFlag = 1 AND SUM(CASE WHEN r.status = 'RESOLVED' THEN 1 ELSE 0 END) > :threshold) " +
            "  OR (:blockEligibleFlag = 0 AND SUM(CASE WHEN r.status = 'RESOLVED' THEN 1 ELSE 0 END) <= :threshold)) " +
            "ORDER BY resolvedReports DESC, totalReports DESC",
            countQuery = "SELECT COUNT(*) FROM (" +
                    "SELECT l.user_id " +
                    "FROM listing_reports r " +
                    "JOIN listings l ON r.listing_id = l.listing_id " +
                    "JOIN users u ON u.user_id = l.user_id " +
                    "WHERE (:email = '' OR u.email LIKE CONCAT(:email, '%')) " +
                    "AND (:name = '' OR u.first_name LIKE CONCAT(:name, '%') OR u.last_name LIKE CONCAT(:name, '%')) " +
                    "AND (:phone = '' OR u.phone_number LIKE CONCAT(:phone, '%')) " +
                    "GROUP BY l.user_id " +
                    "HAVING (:blockEligibleFlag = -1 " +
                    "  OR (:blockEligibleFlag = 1 AND SUM(CASE WHEN r.status = 'RESOLVED' THEN 1 ELSE 0 END) > :threshold) " +
                    "  OR (:blockEligibleFlag = 0 AND SUM(CASE WHEN r.status = 'RESOLVED' THEN 1 ELSE 0 END) <= :threshold))" +
                    ") x",
            nativeQuery = true)
    Page<ReportedAuthorProjection> findReportedAuthors(
            @Param("email") String email,
            @Param("name") String name,
            @Param("phone") String phone,
            @Param("blockEligibleFlag") int blockEligibleFlag,
            @Param("threshold") int threshold,
            Pageable pageable);

    /**
     * Find all reports for a given author (owner of the reported listings) filtered by status,
     * newest first. Used to show an author's admin-approved reports.
     */
    @Query("SELECT r FROM listing_reports r WHERE r.listing.userId = :userId AND r.status = :status " +
            "ORDER BY r.resolvedAt DESC, r.createdAt DESC")
    List<ListingReport> findByAuthorIdAndStatus(@Param("userId") String userId,
                                                @Param("status") ReportStatus status);

    /**
     * Count reports of a given status across all of an author's listings.
     */
    @Query("SELECT COUNT(r) FROM listing_reports r WHERE r.listing.userId = :userId AND r.status = :status")
    long countByAuthorIdAndStatus(@Param("userId") String userId, @Param("status") ReportStatus status);

    /**
     * Total and admin-approved (RESOLVED) report counts for a single author, in one query.
     */
    @Query(value = "SELECT " +
            "CAST(COUNT(*) AS SIGNED) AS totalReports, " +
            "CAST(SUM(CASE WHEN r.status = 'RESOLVED' THEN 1 ELSE 0 END) AS SIGNED) AS resolvedReports " +
            "FROM listing_reports r JOIN listings l ON r.listing_id = l.listing_id " +
            "WHERE l.user_id = :userId",
            nativeQuery = true)
    ReportedAuthorProjection.Counts getReportCountsByAuthor(@Param("userId") String userId);
}
