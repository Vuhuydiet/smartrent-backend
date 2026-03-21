package com.smartrent.service.analytics.impl;

import com.smartrent.infra.repository.ListingClickDailyRepository;
import com.smartrent.infra.repository.PhoneClickDetailRepository;
import com.smartrent.service.analytics.DailyClickAggregationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DailyClickAggregationServiceImpl implements DailyClickAggregationService {

    PhoneClickDetailRepository phoneClickDetailRepository;
    ListingClickDailyRepository listingClickDailyRepository;

    @Override
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void aggregateYesterdayClicks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Aggregating phone clicks for date: {}", yesterday);

        List<Object[]> rows = phoneClickDetailRepository.countClicksGroupedByListingAndDate(yesterday);

        for (Object[] row : rows) {
            Long listingId = ((Number) row[0]).longValue();
            int clickCount = ((Number) row[2]).intValue();

            listingClickDailyRepository.upsertDailyCount(listingId, yesterday, clickCount);
        }

        log.info("Aggregated {} listing click records for {}", rows.size(), yesterday);
    }
}
