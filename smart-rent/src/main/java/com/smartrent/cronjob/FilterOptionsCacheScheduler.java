package com.smartrent.cronjob;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.listing.ListingFilterBucketDefinitions;
import com.smartrent.service.listing.ListingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the "first load, no filters yet" sidebar filter-bucket counts warm
 * (listing.filter-options.baseline — permanent TTL, see application.yml).
 * POST /v1/listings/filter-options runs up to ~14 COUNT(*) queries; without
 * this, every visitor landing on /properties for a (province, productType)
 * combo pays that cost synchronously on their first request.
 *
 * <p>Mirrors {@link HomepageStatsCacheScheduler}: refreshed once a day (offset
 * 15 minutes later so the two jobs don't contend for the DB at the same
 * instant), plus warmed on startup so a Redis flush/fresh deploy doesn't leave
 * the first visitor cold. The warm set — every (province, productType) pair
 * from {@link ListingFilterBucketDefinitions#BASELINE_WARM_PROVINCE_IDS}, plus
 * one "no productType" entry per province — MUST stay exhaustive: it is the
 * only thing standing between a request and a permanently-stale cache entry
 * (see {@code ListingSearchController#isBaselineFilterOptionsRequest}, which
 * only routes requests that are guaranteed to be in this exact set).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FilterOptionsCacheScheduler {

    ListingService listingService;

    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void refreshBaseline() {
        List<ListingFilterRequest> warmSet = buildWarmSet();
        log.info("=== Refreshing filter-options baseline cache ({} entries, daily 00:15) ===", warmSet.size());
        warmSet.forEach(listingService::refreshFilterOptionsBaseline);
        log.info("=== Filter-options baseline cache refreshed ===");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmOnStartup() {
        try {
            List<ListingFilterRequest> warmSet = buildWarmSet();
            log.info("Warming filter-options baseline cache on startup ({} entries)", warmSet.size());
            warmSet.forEach(listingService::getFilterOptionsBaseline);
        } catch (Exception e) {
            log.warn("Startup warm of filter-options baseline cache failed (will populate lazily): {}",
                    e.getMessage());
        }
    }

    private static List<ListingFilterRequest> buildWarmSet() {
        List<ListingFilterRequest> requests = new ArrayList<>();
        for (String provinceId : ListingFilterBucketDefinitions.BASELINE_WARM_PROVINCE_IDS) {
            requests.add(baselineRequest(provinceId, null));
            for (Listing.ProductType productType : Listing.ProductType.values()) {
                requests.add(baselineRequest(provinceId, productType.name()));
            }
        }
        return requests;
    }

    private static ListingFilterRequest baselineRequest(String provinceId, String productType) {
        return ListingFilterRequest.builder()
                .provinceId(provinceId)
                .isLegacy(true)
                .productType(productType)
                .build();
    }
}
