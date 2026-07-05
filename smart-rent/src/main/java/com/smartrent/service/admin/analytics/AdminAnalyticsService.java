package com.smartrent.service.admin.analytics;

import com.smartrent.dto.response.AdminListingAnalyticsResponse;
import com.smartrent.dto.response.AdminReportAnalyticsResponse;
import com.smartrent.dto.response.AdminUserAnalyticsResponse;
import com.smartrent.dto.response.MembershipDistributionResponse;
import com.smartrent.dto.response.RevenueOverTimeResponse;

import java.time.LocalDate;

public interface AdminAnalyticsService {

    RevenueOverTimeResponse getRevenueOverTime(LocalDate from, LocalDate to);

    RevenueOverTimeResponse getRevenueOverTime(int days);

    MembershipDistributionResponse getMembershipDistribution();

    AdminUserAnalyticsResponse getUserAnalytics(int days);

    AdminUserAnalyticsResponse getUserAnalytics(LocalDate from, LocalDate to);

    AdminReportAnalyticsResponse getReportAnalytics(int days);

    AdminReportAnalyticsResponse getReportAnalytics(LocalDate from, LocalDate to);

    AdminListingAnalyticsResponse getListingAnalytics(int days);

    AdminListingAnalyticsResponse getListingAnalytics(LocalDate from, LocalDate to);
}
