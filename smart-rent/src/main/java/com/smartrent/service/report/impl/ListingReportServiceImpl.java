package com.smartrent.service.report.impl;

import com.smartrent.dto.request.ListingReportRequest;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.ReportReasonResponse;
import com.smartrent.enums.ReportCategory;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.ResourceNotFoundException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ListingReportRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.ReportReasonRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingReport;
import com.smartrent.infra.repository.entity.ReportReason;
import com.smartrent.mapper.ListingReportMapper;
import com.smartrent.service.report.ListingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}

