package com.smartrent.service.dashboard;

import com.smartrent.dto.response.MembershipDistributionResponse;
import com.smartrent.dto.response.RevenueOverTimeResponse;
import com.smartrent.dto.response.TimeSeriesResponse;

import java.time.LocalDate;

public interface AdminDashboardService {

    RevenueOverTimeResponse getRevenueOverTime(LocalDate from, LocalDate to);

    RevenueOverTimeResponse getRevenueOverTime(int days);

    MembershipDistributionResponse getMembershipDistribution();

    TimeSeriesResponse getUserGrowth(int days);

    TimeSeriesResponse getReportCount(int days);

    TimeSeriesResponse getListingCreation(int days);
}
