package com.smartrent.mapper;

import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.ReportReasonResponse;
import com.smartrent.infra.repository.entity.ListingReport;
import com.smartrent.infra.repository.entity.ReportReason;

public interface ListingReportMapper {
    
    ReportReasonResponse mapFromReportReasonEntityToResponse(ReportReason reportReason);
    
    ListingReportResponse mapFromListingReportEntityToResponse(ListingReport listingReport);
}

