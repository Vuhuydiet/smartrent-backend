package com.smartrent.service.report.impl;

import com.smartrent.dto.request.ListingReportRequest;
import com.smartrent.dto.request.ResolveReportRequest;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.ReportReasonResponse;
import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.enums.ReportCategory;
import com.smartrent.enums.ReportStatus;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.ResourceNotFoundException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.ListingReportRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.ReportReasonRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingReport;
import com.smartrent.infra.repository.entity.ReportReason;
import com.smartrent.mapper.ListingReportMapper;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.service.email.EmailService;
import com.smartrent.service.moderation.ListingModerationService;
import com.smartrent.service.notification.NotificationService;
import com.smartrent.utility.ModerationEmailBuilder;
import com.smartrent.service.report.ListingReportService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final ListingModerationService listingModerationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @NonFinal
    @Value("${application.email.sender.email}")
    String senderEmail;

    @NonFinal
    @Value("${application.email.sender.name}")
    String senderName;

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

        // Realtime notification: notify all admins about new report
        notificationService.sendToAllAdmins(
                NotificationType.NEW_REPORT,
                "New listing report",
                "A new report has been submitted for listing: " + listing.getTitle(),
                savedReport.getReportId(), "REPORT");

        // Realtime notification: notify listing owner about the report
        if (listing.getUserId() != null) {
            notificationService.sendNotification(
                    listing.getUserId(), RecipientType.USER,
                    NotificationType.NEW_REPORT,
                    "Your listing has been reported",
                    "Your listing \"" + listing.getTitle() + "\" has received a new report.",
                    listing.getListingId(), "LISTING");
        }

        return listingReportMapper.mapFromListingReportEntityToResponse(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
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

        // Notify the reporter about the resolution
        sendReporterNotificationEmail(savedReport, newStatus);

        // Notify the listing owner about the resolution
        sendOwnerNotificationEmail(savedReport, newStatus);

        // Realtime notification: notify reporter
        NotificationType reporterNotifType = (newStatus == ReportStatus.RESOLVED)
                ? NotificationType.REPORT_RESOLVED : NotificationType.REPORT_REJECTED;
        if (report.getReporterEmail() != null) {
            userRepository.findByEmail(report.getReporterEmail()).ifPresent(reporter ->
                    notificationService.sendNotification(
                            reporter.getUserId(), RecipientType.USER,
                            reporterNotifType,
                            "Your report has been reviewed",
                            "Your report #" + reportId + " has been " + newStatus.name().toLowerCase() + ".",
                            reportId, "REPORT"));
        }

        // Realtime notification: notify listing owner
        Listing listing = listingRepository.findById(report.getListingId()).orElse(null);
        if (listing != null && listing.getUserId() != null) {
            notificationService.sendNotification(
                    listing.getUserId(), RecipientType.USER,
                    reporterNotifType,
                    "Report on your listing has been reviewed",
                    "A report on your listing \"" + listing.getTitle() + "\" has been " + newStatus.name().toLowerCase() + ".",
                    listing.getListingId(), "LISTING");
        }

        // If admin resolved and owner action is required, delegate to moderation service
        if (newStatus == ReportStatus.RESOLVED && Boolean.TRUE.equals(request.getOwnerActionRequired())) {
            listingModerationService.handleReportResolutionOwnerAction(
                    reportId, report.getListingId(), request, adminId);
            log.info("Owner action created for report {} on listing {}", reportId, report.getListingId());
        }

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
     * Send notification email to the reporter about the report resolution.
     * Wrapped in try-catch so email failures never break the transaction.
     */
    private void sendReporterNotificationEmail(ListingReport report, ReportStatus status) {
        try {
            if (report.getReporterEmail() == null || report.getReporterEmail().isBlank()) {
                log.warn("No reporter email for report {}, skipping notification", report.getReportId());
                return;
            }

            String listingTitle = listingRepository.findById(report.getListingId())
                    .map(Listing::getTitle)
                    .orElse("(unknown listing)");

            String htmlContent = ModerationEmailBuilder.buildReportResolvedForReporterEmail(
                    listingTitle,
                    report.getReporterName(),
                    status.name(),
                    report.getAdminNotes());

            EmailRequest emailRequest = EmailRequest.builder()
                    .sender(EmailInfo.builder().name(senderName).email(senderEmail).build())
                    .to(List.of(EmailInfo.builder()
                            .name(report.getReporterName())
                            .email(report.getReporterEmail())
                            .build()))
                    .subject("Your report has been reviewed - SmartRent")
                    .htmlContent(htmlContent)
                    .build();

            emailService.sendEmail(emailRequest);
            log.info("Reporter notification email sent to {} for report {}", report.getReporterEmail(), report.getReportId());
        } catch (Exception e) {
            log.warn("Failed to send reporter notification email for report {}: {}", report.getReportId(), e.getMessage());
        }
    }

    /**
     * Send notification email to the listing owner about the report resolution.
     * Wrapped in try-catch so email failures never break the transaction.
     */
    private void sendOwnerNotificationEmail(ListingReport report, ReportStatus status) {
        try {
            Listing listing = listingRepository.findById(report.getListingId()).orElse(null);
            if (listing == null || listing.getUserId() == null) {
                log.warn("Cannot find listing or owner for report {}, skipping owner notification", report.getReportId());
                return;
            }

            var userEntity = userRepository.findById(listing.getUserId()).orElse(null);
            if (userEntity == null || userEntity.getEmail() == null) {
                log.warn("No owner email found for listing {}, skipping owner notification", listing.getListingId());
                return;
            }

            String htmlContent = ModerationEmailBuilder.buildReportResolvedForOwnerEmail(
                    listing.getTitle(),
                    userEntity.getFirstName(),
                    status.name(),
                    report.getAdminNotes());

            EmailRequest emailRequest = EmailRequest.builder()
                    .sender(EmailInfo.builder().name(senderName).email(senderEmail).build())
                    .to(List.of(EmailInfo.builder()
                            .name(userEntity.getFirstName())
                            .email(userEntity.getEmail())
                            .build()))
                    .subject("Report on your listing has been reviewed - SmartRent")
                    .htmlContent(htmlContent)
                    .build();

            emailService.sendEmail(emailRequest);
            log.info("Owner notification email sent to {} for report {} on listing {}",
                    userEntity.getEmail(), report.getReportId(), listing.getListingId());
        } catch (Exception e) {
            log.warn("Failed to send owner notification email for report {}: {}", report.getReportId(), e.getMessage());
        }
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

