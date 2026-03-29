package com.smartrent.service.dashboard.impl;

import com.smartrent.dto.response.*;
import com.smartrent.infra.repository.ListingReportRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.UserMembershipRepository;
import com.smartrent.infra.repository.UserRepository;
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
    UserRepository userRepository;
    ListingReportRepository listingReportRepository;
    ListingRepository listingRepository;

    private static final String GRANULARITY_DAY = "DAY";
    private static final String GRANULARITY_MONTH = "MONTH";

    // ─── Revenue ───

    @Override
    public RevenueOverTimeResponse getRevenueOverTime(LocalDate from, LocalDate to) {
        return buildRevenueResponse(from, to, GRANULARITY_DAY);
    }

    @Override
    public RevenueOverTimeResponse getRevenueOverTime(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        String granularity = days > 30 ? GRANULARITY_MONTH : GRANULARITY_DAY;
        return buildRevenueResponse(startDate, endDate, granularity);
    }

    private RevenueOverTimeResponse buildRevenueResponse(LocalDate from, LocalDate to, String granularity) {
        LocalDateTime startDate = from.atStartOfDay();
        LocalDateTime endDate = to.atTime(LocalTime.MAX);

        log.info("Fetching revenue data from {} to {} (granularity={})", startDate, endDate, granularity);

        List<Object[]> timeRows = GRANULARITY_MONTH.equals(granularity)
                ? transactionRepository.findMonthlyRevenue(startDate, endDate)
                : transactionRepository.findDailyRevenue(startDate, endDate);

        List<RevenueDataPoint> dataPoints = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        long totalTransactions = 0;

        for (Object[] row : timeRows) {
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
                .granularity(granularity)
                .build();
    }

    // ─── Membership Distribution ───

    @Override
    public MembershipDistributionResponse getMembershipDistribution() {
        LocalDateTime now = LocalDateTime.now();

        log.info("Fetching active membership distribution");

        List<Object[]> rows = userMembershipRepository.countActiveGroupedByPackageLevel(now);
        List<MembershipDistributionItem> distribution = new ArrayList<>();
        long totalActive = 0;

        for (Object[] row : rows) {
            Long count = ((Number) row[2]).longValue();
            totalActive += count;
        }

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

    // ─── User Growth ───

    @Override
    public TimeSeriesResponse getUserGrowth(int days) {
        log.info("Fetching user growth for last {} days", days);
        DateRange range = resolveDateRange(days);
        List<Object[]> rows = range.monthly
                ? userRepository.countNewUsersByMonth(range.start, range.end)
                : userRepository.countNewUsersByDay(range.start, range.end);
        return buildTimeSeriesResponse(rows, range.monthly);
    }

    // ─── Report Count ───

    @Override
    public TimeSeriesResponse getReportCount(int days) {
        log.info("Fetching report count for last {} days", days);
        DateRange range = resolveDateRange(days);
        List<Object[]> rows = range.monthly
                ? listingReportRepository.countReportsByMonth(range.start, range.end)
                : listingReportRepository.countReportsByDay(range.start, range.end);
        return buildTimeSeriesResponse(rows, range.monthly);
    }

    // ─── Listing Creation ───

    @Override
    public TimeSeriesResponse getListingCreation(int days) {
        log.info("Fetching listing creation for last {} days", days);
        DateRange range = resolveDateRange(days);
        List<Object[]> rows = range.monthly
                ? listingRepository.countNewListingsByMonth(range.start, range.end)
                : listingRepository.countNewListingsByDay(range.start, range.end);
        return buildTimeSeriesResponse(rows, range.monthly);
    }

    // ─── Helpers ───

    private TimeSeriesResponse buildTimeSeriesResponse(List<Object[]> rows, boolean monthly) {
        List<TimeSeriesDataPoint> dataPoints = new ArrayList<>();
        long total = 0;

        for (Object[] row : rows) {
            String label = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            dataPoints.add(TimeSeriesDataPoint.builder().label(label).count(count).build());
            total += count;
        }

        return TimeSeriesResponse.builder()
                .dataPoints(dataPoints)
                .total(total)
                .granularity(monthly ? GRANULARITY_MONTH : GRANULARITY_DAY)
                .build();
    }

    private DateRange resolveDateRange(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return new DateRange(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                days > 30
        );
    }

    private record DateRange(LocalDateTime start, LocalDateTime end, boolean monthly) {}
}
