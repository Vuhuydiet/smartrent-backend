package com.smartrent.mapper.impl;

import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.ReportReasonResponse;
import com.smartrent.infra.repository.entity.ListingReport;
import com.smartrent.infra.repository.entity.ReportReason;
import com.smartrent.mapper.ListingReportMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ListingReportMapperImpl implements ListingReportMapper {

    @Override
    public ReportReasonResponse mapFromReportReasonEntityToResponse(ReportReason reportReason) {
        return ReportReasonResponse.builder()
                .reasonId(reportReason.getReasonId())
                .reasonText(reportReason.getReasonText())
                .category(reportReason.getCategory().name())
                .displayOrder(reportReason.getDisplayOrder())
                .build();
    }

    @Override
    public ListingReportResponse mapFromListingReportEntityToResponse(ListingReport listingReport) {
        return ListingReportResponse.builder()
                .reportId(listingReport.getReportId())
                .listingId(listingReport.getListingId())
                .reporterName(listingReport.getReporterName())
                .reporterPhone(listingReport.getReporterPhone())
                .reporterEmail(listingReport.getReporterEmail())
                .reportReasons(listingReport.getReportReasons().stream()
                        .map(this::mapFromReportReasonEntityToResponse)
                        .collect(Collectors.toList()))
                .otherFeedback(listingReport.getOtherFeedback())
                .category(listingReport.getCategory().name())
                .status(listingReport.getStatus().name())
                .resolvedBy(listingReport.getResolvedBy())
                .resolvedAt(listingReport.getResolvedAt())
                .adminNotes(listingReport.getAdminNotes())
                .createdAt(listingReport.getCreatedAt())
                .updatedAt(listingReport.getUpdatedAt())
                .build();
    }
}

