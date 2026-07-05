package com.smartrent.service.admin.analytics.impl;

import com.smartrent.dto.response.*;
import com.smartrent.infra.repository.ListingReportRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.UserMembershipRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.service.admin.analytics.AdminAnalyticsService;
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
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

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

    // ─── User Analytics ───

    @Override
    public AdminUserAnalyticsResponse getUserAnalytics(int days) {
        log.info("Fetching user analytics for last {} days", days);
        DateRange range = resolveDateRange(days);
        return buildUserAnalyticsResponse(range.start, range.end, range.monthly);
    }

    @Override
    public AdminUserAnalyticsResponse getUserAnalytics(LocalDate from, LocalDate to) {
        log.info("Fetching user analytics from {} to {}", from, to);
        DateRange range = resolveDateRange(from, to);
        return buildUserAnalyticsResponse(range.start, range.end, range.monthly);
    }

    private AdminUserAnalyticsResponse buildUserAnalyticsResponse(LocalDateTime start, LocalDateTime end, boolean monthly) {
        List<Object[]> rows = monthly
                ? userRepository.countNewUsersByMonth(start, end)
                : userRepository.countNewUsersByDay(start, end);
        SeriesResult series = toSeries(rows);

        long baseline = userRepository.countByCreatedAtBefore(start);
        List<TimeSeriesDataPoint> cumulative = buildCumulativeSeries(series.dataPoints(), baseline);

        List<Object[]> roleRows = userRepository.countNewUsersByRole(start, end);
        List<Object[]> brokerVerificationRows = userRepository.countNewBrokersByVerificationStatus(start, end);

        return AdminUserAnalyticsResponse.builder()
                .dataPoints(series.dataPoints())
                .total(series.total())
                .granularity(monthly ? GRANULARITY_MONTH : GRANULARITY_DAY)
                .cumulativeDataPoints(cumulative)
                .totalUsersAsOfRangeEnd(baseline + series.total())
                .roleBreakdown(buildBreakdown(roleRows))
                .brokerVerificationBreakdown(buildBreakdown(brokerVerificationRows))
                .build();
    }

    // ─── Report Analytics ───

    @Override
    public AdminReportAnalyticsResponse getReportAnalytics(int days) {
        log.info("Fetching report analytics for last {} days", days);
        DateRange range = resolveDateRange(days);
        return buildReportAnalyticsResponse(range.start, range.end, range.monthly);
    }

    @Override
    public AdminReportAnalyticsResponse getReportAnalytics(LocalDate from, LocalDate to) {
        log.info("Fetching report analytics from {} to {}", from, to);
        DateRange range = resolveDateRange(from, to);
        return buildReportAnalyticsResponse(range.start, range.end, range.monthly);
    }

    private AdminReportAnalyticsResponse buildReportAnalyticsResponse(LocalDateTime start, LocalDateTime end, boolean monthly) {
        List<Object[]> rows = monthly
                ? listingReportRepository.countReportsByMonth(start, end)
                : listingReportRepository.countReportsByDay(start, end);
        SeriesResult series = toSeries(rows);

        long baseline = listingReportRepository.countByCreatedAtBefore(start);
        List<TimeSeriesDataPoint> cumulative = buildCumulativeSeries(series.dataPoints(), baseline);

        List<Object[]> categoryRows = listingReportRepository.countReportsByCategory(start, end);
        List<Object[]> statusRows = listingReportRepository.countReportsByStatus(start, end);
        List<CategoryBreakdownItem> statusBreakdown = buildBreakdown(statusRows);

        long resolvedOrRejected = 0;
        for (Object[] row : statusRows) {
            String status = (String) row[0];
            if ("RESOLVED".equals(status) || "REJECTED".equals(status)) {
                resolvedOrRejected += ((Number) row[1]).longValue();
            }
        }
        double resolutionRate = series.total() > 0
                ? round2(resolvedOrRejected * 100.0 / series.total())
                : 0.0;

        Double avgMinutes = listingReportRepository.avgResolutionMinutes(start, end);
        Double avgHours = avgMinutes != null ? round2(avgMinutes / 60.0) : null;

        return AdminReportAnalyticsResponse.builder()
                .dataPoints(series.dataPoints())
                .total(series.total())
                .granularity(monthly ? GRANULARITY_MONTH : GRANULARITY_DAY)
                .cumulativeDataPoints(cumulative)
                .categoryBreakdown(buildBreakdown(categoryRows))
                .statusBreakdown(statusBreakdown)
                .resolutionRatePercent(resolutionRate)
                .avgResolutionHours(avgHours)
                .build();
    }

    // ─── Listing Analytics ───

    @Override
    public AdminListingAnalyticsResponse getListingAnalytics(int days) {
        log.info("Fetching listing analytics for last {} days", days);
        DateRange range = resolveDateRange(days);
        return buildListingAnalyticsResponse(range.start, range.end, range.monthly);
    }

    @Override
    public AdminListingAnalyticsResponse getListingAnalytics(LocalDate from, LocalDate to) {
        log.info("Fetching listing analytics from {} to {}", from, to);
        DateRange range = resolveDateRange(from, to);
        return buildListingAnalyticsResponse(range.start, range.end, range.monthly);
    }

    private AdminListingAnalyticsResponse buildListingAnalyticsResponse(LocalDateTime start, LocalDateTime end, boolean monthly) {
        List<Object[]> rows = monthly
                ? listingRepository.countNewListingsByMonth(start, end)
                : listingRepository.countNewListingsByDay(start, end);
        SeriesResult series = toSeries(rows);

        long baseline = listingRepository.countByCreatedAtBeforeAndIsDraftFalseAndIsShadowFalse(start);
        List<TimeSeriesDataPoint> cumulative = buildCumulativeSeries(series.dataPoints(), baseline);

        List<Object[]> typeRows = listingRepository.countNewListingsByType(start, end);
        List<Object[]> productRows = listingRepository.countNewListingsByProductType(start, end);
        List<Object[]> verificationRows = listingRepository.countNewListingsByVerification(start, end);

        return AdminListingAnalyticsResponse.builder()
                .dataPoints(series.dataPoints())
                .total(series.total())
                .granularity(monthly ? GRANULARITY_MONTH : GRANULARITY_DAY)
                .cumulativeDataPoints(cumulative)
                .totalListingsAsOfRangeEnd(baseline + series.total())
                .listingTypeBreakdown(buildBreakdown(typeRows))
                .productTypeBreakdown(buildBreakdown(productRows))
                .verificationBreakdown(buildBreakdown(verificationRows))
                .build();
    }

    // ─── Helpers ───

    private SeriesResult toSeries(List<Object[]> rows) {
        List<TimeSeriesDataPoint> dataPoints = new ArrayList<>();
        long total = 0;

        for (Object[] row : rows) {
            String label = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            dataPoints.add(TimeSeriesDataPoint.builder().label(label).count(count).build());
            total += count;
        }

        return new SeriesResult(dataPoints, total);
    }

    private List<TimeSeriesDataPoint> buildCumulativeSeries(List<TimeSeriesDataPoint> dataPoints, long baseline) {
        List<TimeSeriesDataPoint> result = new ArrayList<>();
        long running = baseline;

        for (TimeSeriesDataPoint dp : dataPoints) {
            running += dp.getCount();
            result.add(TimeSeriesDataPoint.builder().label(dp.getLabel()).count(running).build());
        }

        return result;
    }

    private List<CategoryBreakdownItem> buildBreakdown(List<Object[]> rows) {
        List<CategoryBreakdownItem> items = new ArrayList<>();
        long total = 0;

        for (Object[] row : rows) {
            total += ((Number) row[1]).longValue();
        }

        for (Object[] row : rows) {
            String category = String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            double percentage = total > 0 ? round2(count * 100.0 / total) : 0.0;

            items.add(CategoryBreakdownItem.builder()
                    .category(category)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        return items;
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
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

    private DateRange resolveDateRange(LocalDate from, LocalDate to) {
        return new DateRange(
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX),
                false
        );
    }

    private record DateRange(LocalDateTime start, LocalDateTime end, boolean monthly) {}

    private record SeriesResult(List<TimeSeriesDataPoint> dataPoints, long total) {}
}
