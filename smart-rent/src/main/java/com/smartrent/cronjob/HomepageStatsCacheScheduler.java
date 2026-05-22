package com.smartrent.cronjob;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.CategoryStatsRequest;
import com.smartrent.dto.request.ProvinceStatsRequest;
import com.smartrent.service.listing.ListingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Keeps the two homepage stats caches warm:
 *  - "Bất động sản theo danh mục" → {@code listing.stats.categories}
 *  - "Bất động sản theo địa điểm"  → {@code listing.stats.provinces}
 *
 * <p>Both caches are configured with no TTL (see application.yml), i.e. they
 * live in Redis permanently. Counting public listings per province/category is
 * expensive (one COUNT query each), so we don't want it to run on the hot path.
 * Instead this scheduler refreshes the numbers once a day at midnight: it
 * evicts the cached entries and immediately recomputes them by calling the
 * {@code @Cacheable} service methods, which repopulates Redis with fresh data.
 *
 * <p>The warm-up parameters MUST mirror exactly what the homepage sends, because
 * the cache key is derived from the request. They are the fixed constants from
 * the frontend homepage ({@code smartrent-fe/src/pages/index.tsx} —
 * {@code TOP_PROVINCE_IDS}/{@code TOP_PROVINCE_CODES} and the 5 category ids).
 * If the homepage changes its set, update these too — otherwise the cron warms
 * a key nobody reads and the homepage falls back to a lazy (cold) recompute.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HomepageStatsCacheScheduler {

    /** Categories shown on the homepage "by category" section. */
    private static final List<Long> HOMEPAGE_CATEGORY_IDS = List.of(1L, 2L, 3L, 4L, 5L);
    /** Top provinces shown on the homepage "by location" section (legacy ids). */
    private static final List<Integer> HOMEPAGE_PROVINCE_IDS = List.of(1, 49, 32, 47, 48);
    /** New-structure codes for the same provinces, matching the ids above by position. */
    private static final List<String> HOMEPAGE_PROVINCE_CODES = List.of("1", "79", "48", "74", "75");
    private static final String HOMEPAGE_ADDRESS_TYPE = "NEW";

    ListingService listingService;
    CacheManager cacheManager;

    /**
     * Runs every day at 00:00 Asia/Ho_Chi_Minh. Evicts then recomputes both
     * homepage stats caches so the permanent Redis entries carry fresh numbers
     * for the rest of the day.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void refreshHomepageStats() {
        log.info("=== Refreshing homepage stats caches (daily 00:00) ===");
        evictAndWarm();
        log.info("=== Homepage stats caches refreshed ===");
    }

    /**
     * Populate the caches on startup if Redis was flushed or this is a fresh
     * deploy, so the first homepage visitor doesn't pay the cold cost. A failure
     * here is logged but never blocks application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmOnStartup() {
        try {
            log.info("Warming homepage stats caches on startup");
            warm();
        } catch (Exception e) {
            log.warn("Startup warm of homepage stats caches failed (will populate lazily): {}",
                    e.getMessage());
        }
    }

    private void evictAndWarm() {
        clearCache(Constants.CacheNames.LISTING_STATS_CATEGORIES);
        clearCache(Constants.CacheNames.LISTING_STATS_PROVINCES);
        warm();
    }

    /** Calls the {@code @Cacheable} service methods so Redis is repopulated. */
    private void warm() {
        listingService.getCategoryStats(buildCategoryRequest());
        listingService.getProvinceStats(buildProvinceRequest());
    }

    private void clearCache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache != null) {
            cache.clear();
        }
    }

    private static CategoryStatsRequest buildCategoryRequest() {
        return CategoryStatsRequest.builder()
                .categoryIds(HOMEPAGE_CATEGORY_IDS)
                .verifiedOnly(true)
                .build();
    }

    private static ProvinceStatsRequest buildProvinceRequest() {
        return ProvinceStatsRequest.builder()
                .provinceIds(HOMEPAGE_PROVINCE_IDS)
                .provinceCodes(HOMEPAGE_PROVINCE_CODES)
                .addressType(HOMEPAGE_ADDRESS_TYPE)
                .verifiedOnly(true)
                .build();
    }
}
