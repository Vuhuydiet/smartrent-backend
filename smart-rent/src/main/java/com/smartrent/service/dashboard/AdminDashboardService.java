package com.smartrent.service.dashboard;

import com.smartrent.dto.response.MembershipDistributionResponse;
import com.smartrent.dto.response.RevenueOverTimeResponse;

import java.time.LocalDate;

public interface AdminDashboardService {

    RevenueOverTimeResponse getRevenueOverTime(LocalDate from, LocalDate to);

    MembershipDistributionResponse getMembershipDistribution();
}
