package com.smartrent.service.analytics.impl;

import com.smartrent.dto.response.DailyClickCount;
import com.smartrent.dto.response.DailySaveCount;
import com.smartrent.dto.response.ListingAnalyticsResponse;
import com.smartrent.dto.response.ListingClickSummary;
import com.smartrent.dto.response.ListingSaveSummary;
import com.smartrent.dto.response.OwnerListingsAnalyticsResponse;
import com.smartrent.dto.response.OwnerSavedListingsAnalyticsResponse;
import com.smartrent.dto.response.SavedListingsTrendResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.PhoneClickDetailRepository;
import com.smartrent.infra.repository.SavedListingRepository;
import com.smartrent.infra.repository.ViewRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.analytics.AnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    PhoneClickDetailRepository phoneClickDetailRepository;
    ViewRepository viewRepository;
    ListingRepository listingRepository;
    SavedListingRepository savedListingRepository;

    @Override
    @Transactional(readOnly = true)
    public ListingAnalyticsResponse getListingAnalytics(Long listingId, String ownerId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (!listing.getUserId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this listing");
        }

        long totalClicks = phoneClickDetailRepository.countByListing_ListingId(listingId);
        long totalViews = viewRepository.countByListing_ListingId(listingId);
        double conversionRate = totalViews > 0 ? (double) totalClicks / totalViews : 0.0;

        List<DailyClickCount> clicksOverTime = buildClicksOverTime(listingId);
        Map<String, Long> clicksByDayOfWeek = buildClicksByDayOfWeek(listingId);

        return ListingAnalyticsResponse.builder()
                .listingId(listingId)
                .listingTitle(listing.getTitle())
                .totalClicks(totalClicks)
                .totalViews(totalViews)
                .conversionRate(Math.round(conversionRate * 10000.0) / 10000.0)
                .clicksOverTime(clicksOverTime)
                .clicksByDayOfWeek(clicksByDayOfWeek)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerListingsAnalyticsResponse getOwnerListingsAnalytics(String ownerId) {
        List<Object[]> rawCounts = phoneClickDetailRepository.countClicksPerListingForOwner(ownerId);

        List<ListingClickSummary> summaries = rawCounts.stream()
                .map(row -> {
                    Long listingId = (Long) row[0];
                    Long count = (Long) row[1];
                    String title = listingRepository.findById(listingId)
                            .map(Listing::getTitle)
                            .orElse("Unknown");
                    return ListingClickSummary.builder()
                            .listingId(listingId)
                            .listingTitle(title)
                            .totalClicks(count)
                            .build();
                })
                .collect(Collectors.toList());

        return OwnerListingsAnalyticsResponse.builder()
                .listings(summaries)
                .build();
    }

    private List<DailyClickCount> buildClicksOverTime(Long listingId) {
        List<Object[]> rawData = phoneClickDetailRepository.countClicksGroupedByDate(listingId);
        return rawData.stream()
                .map(row -> DailyClickCount.builder()
                        .date((LocalDate) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, Long> buildClicksByDayOfWeek(Long listingId) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            result.put(day.name().substring(0, 3), 0L);
        }

        List<Object[]> rawData = phoneClickDetailRepository.countClicksGroupedByDayOfWeek(listingId);
        for (Object[] row : rawData) {
            int mysqlDow = ((Number) row[0]).intValue();
            Long count = (Long) row[1];
            String dayName = mysqlDayOfWeekToName(mysqlDow);
            result.put(dayName, count);
        }

        return result;
    }

    private String mysqlDayOfWeekToName(int mysqlDow) {
        return switch (mysqlDow) {
            case 1 -> "SUN";
            case 2 -> "MON";
            case 3 -> "TUE";
            case 4 -> "WED";
            case 5 -> "THU";
            case 6 -> "FRI";
            case 7 -> "SAT";
            default -> "UNK";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public SavedListingsTrendResponse getSavedListingTrend(Long listingId, String ownerId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (!listing.getUserId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this listing");
        }

        long totalSaves = savedListingRepository.countByIdListingId(listingId);

        List<Object[]> rawData = savedListingRepository.countSavesGroupedByDate(listingId);
        List<DailySaveCount> savesOverTime = rawData.stream()
                .map(row -> DailySaveCount.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return SavedListingsTrendResponse.builder()
                .listingId(listingId)
                .listingTitle(listing.getTitle())
                .totalSaves(totalSaves)
                .savesOverTime(savesOverTime)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerSavedListingsAnalyticsResponse getOwnerSavedListingsAnalytics(String ownerId) {
        List<Object[]> rawCounts = savedListingRepository.countSavesPerListingForOwner(ownerId);
        long totalSavesAcrossAll = 0;

        List<ListingSaveSummary> summaries = rawCounts.stream()
                .map(row -> {
                    Long lid = ((Number) row[0]).longValue();
                    Long count = ((Number) row[1]).longValue();
                    String title = listingRepository.findById(lid)
                            .map(Listing::getTitle)
                            .orElse("Unknown");
                    return ListingSaveSummary.builder()
                            .listingId(lid)
                            .listingTitle(title)
                            .totalSaves(count)
                            .build();
                })
                .collect(Collectors.toList());

        for (ListingSaveSummary s : summaries) {
            totalSavesAcrossAll += s.getTotalSaves();
        }

        return OwnerSavedListingsAnalyticsResponse.builder()
                .listings(summaries)
                .totalSavesAcrossAll(totalSavesAcrossAll)
                .build();
    }
}
