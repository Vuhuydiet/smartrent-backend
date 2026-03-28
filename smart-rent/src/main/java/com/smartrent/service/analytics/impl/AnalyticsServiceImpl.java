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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public ListingAnalyticsResponse getListingAnalytics(Long listingId, String ownerId, LocalDateTime since) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (!listing.getUserId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this listing");
        }

        long totalClicks;
        long totalViews = viewRepository.countByListing_ListingId(listingId);
        List<DailyClickCount> clicksOverTime;
        Map<String, Long> clicksByDayOfWeek;

        if (since != null) {
            totalClicks = phoneClickDetailRepository.countByListing_ListingIdAndClickedAtBetween(
                    listingId, since, LocalDateTime.now());
            clicksOverTime = buildClicksOverTimeSince(listingId, since);
            clicksByDayOfWeek = buildClicksByDayOfWeekSince(listingId, since);
        } else {
            totalClicks = phoneClickDetailRepository.countByListing_ListingId(listingId);
            clicksOverTime = buildClicksOverTime(listingId);
            clicksByDayOfWeek = buildClicksByDayOfWeek(listingId);
        }

        double conversionRate = totalViews > 0 ? (double) totalClicks / totalViews : 0.0;

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
    public OwnerListingsAnalyticsResponse getOwnerListingsAnalytics(String ownerId, Pageable pageable) {
        Page<Object[]> page = phoneClickDetailRepository.countClicksPerListingForOwnerPaged(ownerId, pageable);

        List<ListingClickSummary> summaries = page.getContent().stream()
                .map(row -> {
                    Long listingId = ((Number) row[0]).longValue();
                    Long count = ((Number) row[1]).longValue();
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
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .build();
    }

    // ─── Saved Listings Trend ───

    @Override
    @Transactional(readOnly = true)
    public SavedListingsTrendResponse getSavedListingTrend(Long listingId, String ownerId, LocalDateTime since) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (!listing.getUserId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this listing");
        }

        long totalSaves = savedListingRepository.countByIdListingId(listingId);

        List<Object[]> rawData;
        if (since != null) {
            rawData = savedListingRepository.countSavesGroupedByDateSince(listingId, since);
        } else {
            rawData = savedListingRepository.countSavesGroupedByDate(listingId);
        }

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
    public OwnerSavedListingsAnalyticsResponse getOwnerSavedListingsAnalytics(String ownerId, Pageable pageable) {
        Page<Object[]> page = savedListingRepository.countSavesPerListingForOwnerPaged(ownerId, pageable);
        long totalSavesAcrossAll = 0;

        List<ListingSaveSummary> summaries = page.getContent().stream()
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
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .build();
    }

    // ─── Private helpers ───

    private List<DailyClickCount> buildClicksOverTime(Long listingId) {
        List<Object[]> rawData = phoneClickDetailRepository.countClicksGroupedByDate(listingId);
        return rawData.stream()
                .map(row -> DailyClickCount.builder()
                        .date((LocalDate) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    private List<DailyClickCount> buildClicksOverTimeSince(Long listingId, LocalDateTime since) {
        List<Object[]> rawData = phoneClickDetailRepository.countClicksGroupedByDateSince(listingId, since);
        return rawData.stream()
                .map(row -> DailyClickCount.builder()
                        .date((LocalDate) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, Long> buildClicksByDayOfWeek(Long listingId) {
        Map<String, Long> result = initDayOfWeekMap();
        List<Object[]> rawData = phoneClickDetailRepository.countClicksGroupedByDayOfWeek(listingId);
        fillDayOfWeekMap(result, rawData);
        return result;
    }

    private Map<String, Long> buildClicksByDayOfWeekSince(Long listingId, LocalDateTime since) {
        Map<String, Long> result = initDayOfWeekMap();
        List<Object[]> rawData = phoneClickDetailRepository.countClicksGroupedByDayOfWeekSince(listingId, since);
        fillDayOfWeekMap(result, rawData);
        return result;
    }

    private Map<String, Long> initDayOfWeekMap() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            result.put(day.name().substring(0, 3), 0L);
        }
        return result;
    }

    private void fillDayOfWeekMap(Map<String, Long> result, List<Object[]> rawData) {
        for (Object[] row : rawData) {
            int mysqlDow = ((Number) row[0]).intValue();
            Long count = (Long) row[1];
            String dayName = mysqlDayOfWeekToName(mysqlDow);
            result.put(dayName, count);
        }
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
}
