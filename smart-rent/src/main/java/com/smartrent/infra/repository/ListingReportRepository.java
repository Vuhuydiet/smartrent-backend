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
}
