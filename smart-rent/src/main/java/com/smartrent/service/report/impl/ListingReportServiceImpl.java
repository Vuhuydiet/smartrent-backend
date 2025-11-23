package com.smartrent.service.report.impl;

import com.smartrent.dto.request.ListingReportRequest;
import com.smartrent.dto.request.ResolveReportRequest;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.ReportReasonResponse;
import com.smartrent.enums.ReportCategory;
import com.smartrent.enums.ReportStatus;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.ResourceNotFoundException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.ListingReportRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.ReportReasonRepository;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingReport;
import com.smartrent.infra.repository.entity.ReportReason;
import com.smartrent.mapper.ListingReportMapper;
import com.smartrent.service.report.ListingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingReportServiceImpl implements ListingReportService {

    private final ReportReasonRepository reportReasonRepository;
    private final ListingReportRepository listingReportRepository;
    private final ListingRepository listingRepository;
    private final ListingReportMapper listingReportMapper;
    private final AdminRepository adminRepository;

    @Override
    public List<ReportReasonResponse> getReportReasons() {
        log.info("Fetching all active report reasons");
        List<ReportReason> reasons = reportReasonRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return reasons.stream()
                .map(listingReportMapper::mapFromReportReasonEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ListingReportResponse createReport(Long listingId, ListingReportRequest request) {
        log.info("Creating report for listing ID: {} by reporter: {}", listingId, request.getReporterEmail());

        // Validate listing exists
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        // Validate report category
        ReportCategory category;
        try {
            category = ReportCategory.valueOf(request.getCategory());
        } catch (IllegalArgumentException e) {
            throw new DomainException(DomainCode.BAD_REQUEST_ERROR, "Invalid report category: " + request.getCategory());
        }

        // Validate report reasons exist
        List<ReportReason> reportReasons = reportReasonRepository.findAllById(request.getReasonIds());
        if (reportReasons.size() != request.getReasonIds().size()) {
            Set<Long> foundIds = reportReasons.stream()
                    .map(ReportReason::getReasonId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = request.getReasonIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Report reasons not found with IDs: " + missingIds);
        }

        // Validate all reasons are active
        List<ReportReason> inactiveReasons = reportReasons.stream()
                .filter(reason -> !reason.getIsActive())
                .collect(Collectors.toList());
        if (!inactiveReasons.isEmpty()) {
            throw new DomainException(DomainCode.BAD_REQUEST_ERROR, 
                    "Some report reasons are no longer active: " + 
                    inactiveReasons.stream().map(ReportReason::getReasonId).collect(Collectors.toList()));
        }

        // Create and save the report
        ListingReport report = ListingReport.builder()
                .listingId(listingId)
                .reporterName(request.getReporterName())
                .reporterPhone(request.getReporterPhone())
                .reporterEmail(request.getReporterEmail())
                .reportReasons(reportReasons)
                .otherFeedback(request.getOtherFeedback())
                .category(category)
                .build();

        ListingReport savedReport = listingReportRepository.save(report);
        log.info("Report created successfully with ID: {} for listing ID: {}", savedReport.getReportId(), listingId);

        return listingReportMapper.mapFromListingReportEntityToResponse(savedReport);
    }

    @Override
    public List<ListingReportResponse> getReportHistory(Long listingId) {
        log.info("Fetching report history for listing ID: {}", listingId);

        // Validate listing exists
        if (!listingRepository.existsById(listingId)) {
            throw new ResourceNotFoundException("Listing not found with ID: " + listingId);
        }

        List<ListingReport> reports = listingReportRepository.findByListingIdOrderByCreatedAtDesc(listingId);
        return reports.stream()
                .map(listingReportMapper::mapFromListingReportEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getReportCount(Long listingId) {
        log.info("Fetching report count for listing ID: {}", listingId);
        return listingReportRepository.countByListingId(listingId);
    }

    // ============ ADMIN METHODS ============

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ListingReportResponse> getAllReports(String status, int page, int size) {
        log.info("Admin fetching all reports - status: {}, page: {}, size: {}", status, page, size);

        // Convert 1-based page to 0-based for Spring Data
        int safePage = Math.max(page - 1, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<ListingReport> reportPage;
        if (status != null && !status.isEmpty()) {
            try {
                ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
                reportPage = listingReportRepository.findByStatusOrderByCreatedAtDesc(reportStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new DomainException(DomainCode.BAD_REQUEST_ERROR, "Invalid status: " + status);
            }
        } else {
            reportPage = listingReportRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<ListingReportResponse> reports = reportPage.getContent().stream()
                .map(this::mapToResponseWithAdminInfo)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} reports", reports.size());

        return PageResponse.<ListingReportResponse>builder()
                .page(page)
                .size(reportPage.getSize())
                .totalPages(reportPage.getTotalPages())
                .totalElements(reportPage.getTotalElements())
                .data(reports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ListingReportResponse getReportById(Long reportId) {
        log.info("Admin fetching report by ID: {}", reportId);

        ListingReport report = listingReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        return mapToResponseWithAdminInfo(report);
    }

    @Override
    @Transactional
    public ListingReportResponse resolveReport(Long reportId, ResolveReportRequest request, String adminId) {
        log.info("Admin {} resolving report ID: {} with status: {}", adminId, reportId, request.getStatus());

        // Validate admin exists
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));

        // Validate report exists
        ListingReport report = listingReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        // Check if report is already resolved
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new DomainException(DomainCode.BAD_REQUEST_ERROR,
                    "Report has already been " + report.getStatus().name().toLowerCase());
        }

        // Validate status
        ReportStatus newStatus;
        try {
            newStatus = ReportStatus.valueOf(request.getStatus().toUpperCase());
            if (newStatus == ReportStatus.PENDING) {
                throw new DomainException(DomainCode.BAD_REQUEST_ERROR,
                        "Cannot set status back to PENDING");
            }
        } catch (IllegalArgumentException e) {
            throw new DomainException(DomainCode.BAD_REQUEST_ERROR, "Invalid status: " + request.getStatus());
        }

        // Update report
        report.setStatus(newStatus);
        report.setResolvedBy(adminId);
        report.setResolvedAt(LocalDateTime.now());
        report.setAdminNotes(request.getAdminNotes());

        ListingReport savedReport = listingReportRepository.save(report);
        log.info("Report {} successfully {} by admin {}", reportId, newStatus.name().toLowerCase(), adminId);

        return mapToResponseWithAdminInfo(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingReportService.ReportStatistics getReportStatistics() {
        log.info("Fetching report statistics");

        long totalReports = listingReportRepository.count();
        long pendingReports = listingReportRepository.countByStatus(ReportStatus.PENDING);
        long resolvedReports = listingReportRepository.countByStatus(ReportStatus.RESOLVED);
        long rejectedReports = listingReportRepository.countByStatus(ReportStatus.REJECTED);

        log.info("Report statistics - Total: {}, Pending: {}, Resolved: {}, Rejected: {}",
                totalReports, pendingReports, resolvedReports, rejectedReports);

        return new ListingReportService.ReportStatistics(totalReports, pendingReports, resolvedReports, rejectedReports);
    }

    /**
     * Helper method to map report to response with admin information
     */
    private ListingReportResponse mapToResponseWithAdminInfo(ListingReport report) {
        ListingReportResponse response = listingReportMapper.mapFromListingReportEntityToResponse(report);

        // Add admin information if report is resolved
        if (report.getResolvedBy() != null) {
            adminRepository.findById(report.getResolvedBy()).ifPresent(admin -> {
                response.setResolvedByName(admin.getFirstName() + " " + admin.getLastName());
            });
        }

        return response;
    }
}

