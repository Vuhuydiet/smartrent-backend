package com.smartrent.service.dashboard.impl;

import com.smartrent.dto.response.*;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.UserMembershipRepository;
import com.smartrent.service.dashboard.AdminDashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

    TransactionRepository transactionRepository;
    UserMembershipRepository userMembershipRepository;

    @Override
    public RevenueOverTimeResponse getRevenueOverTime(LocalDate from, LocalDate to) {
        LocalDateTime startDate = from.atStartOfDay();
        LocalDateTime endDate = to.atTime(LocalTime.MAX);

        log.info("Fetching revenue data from {} to {}", startDate, endDate);

        // Daily revenue data points
        List<Object[]> dailyRows = transactionRepository.findDailyRevenue(startDate, endDate);
        List<RevenueDataPoint> dataPoints = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        long totalTransactions = 0;

        for (Object[] row : dailyRows) {
            String date = row[0].toString();
            BigDecimal amount = new BigDecimal(row[1].toString());
            Long count = ((Number) row[2]).longValue();

            dataPoints.add(RevenueDataPoint.builder()
                    .date(date)
                    .totalAmount(amount)
                    .transactionCount(count)
                    .build());

            grandTotal = grandTotal.add(amount);
            totalTransactions += count;
        }

        // Revenue breakdown by type
        List<Object[]> typeRows = transactionRepository.findRevenueGroupedByType(startDate, endDate);
        List<RevenueByTypeItem> revenueByType = new ArrayList<>();

        for (Object[] row : typeRows) {
            String type = (String) row[0];
            BigDecimal amount = new BigDecimal(row[1].toString());
            Long count = ((Number) row[2]).longValue();

            revenueByType.add(RevenueByTypeItem.builder()
                    .transactionType(type)
                    .totalAmount(amount)
                    .transactionCount(count)
                    .build());
        }

        return RevenueOverTimeResponse.builder()
                .dataPoints(dataPoints)
                .grandTotal(grandTotal)
                .totalTransactions(totalTransactions)
                .revenueByType(revenueByType)
                .build();
    }

    @Override
    public MembershipDistributionResponse getMembershipDistribution() {
        LocalDateTime now = LocalDateTime.now();

        log.info("Fetching active membership distribution");

        List<Object[]> rows = userMembershipRepository.countActiveGroupedByPackageLevel(now);
        List<MembershipDistributionItem> distribution = new ArrayList<>();
        long totalActive = 0;

        // First pass: collect counts
        for (Object[] row : rows) {
            Long count = ((Number) row[2]).longValue();
            totalActive += count;
        }

        // Second pass: compute percentages
        for (Object[] row : rows) {
            String level = (String) row[0];
            String name = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            double percentage = totalActive > 0
                    ? BigDecimal.valueOf(count)
                        .divide(BigDecimal.valueOf(totalActive), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                    : 0.0;

            distribution.add(MembershipDistributionItem.builder()
                    .packageLevel(level)
                    .packageName(name)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        return MembershipDistributionResponse.builder()
                .distribution(distribution)
                .totalActive(totalActive)
                .build();
    }
}
