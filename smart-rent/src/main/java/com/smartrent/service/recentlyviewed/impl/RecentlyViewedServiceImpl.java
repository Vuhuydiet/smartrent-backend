package com.smartrent.service.recentlyviewed.impl;

import com.smartrent.dto.request.RecentlyViewedItemDto;
import com.smartrent.dto.request.RecentlyViewedSyncRequest;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.RecentlyViewedItemResponse;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.recentlyviewed.RecentlyViewedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecentlyViewedServiceImpl implements RecentlyViewedService {

    private static final String KEY_PREFIX = "recently_viewed:";
    private static final int MAX_LISTINGS = 20;
    private static final long MAX_FUTURE_TIMESTAMP = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000); // 1 year in future

    private final RedisTemplate<String, String> redisTemplate;
    private final ListingService listingService;
    private final CacheManager cacheManager;

    private void evictPersonalizedCache(String userId) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(com.smartrent.config.Constants.CacheNames.LISTING_RECOMMENDATION_PERSONALIZED);
            if (cache != null) {
                cache.evict("user:" + userId + ":topN:8");
                cache.evict("user:" + userId + ":topN:9");
                cache.evict("user:" + userId + ":topN:10");
                cache.evict("user:" + userId + ":topN:11");
                cache.evict("user:" + userId + ":topN:12");
                cache.evict("user:" + userId + ":topN:15");
                cache.evict("user:" + userId + ":topN:20");
                log.info("Successfully evicted recommendation cache for user: {}", userId);
            }
        } catch (Exception e) {
            log.warn("Failed to evict recommendation cache for user " + userId, e);
        }
    }

    @Override
    public List<RecentlyViewedItemResponse> syncRecentlyViewed(RecentlyViewedSyncRequest request) {
        String userId = getCurrentUserId();
        String redisKey = buildKey(userId);

        log.info("Syncing recently viewed listings for user: {}, client listings count: {}",
                userId, request.getListings() != null ? request.getListings().size() : 0);

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // Handle empty client payload gracefully
        if (request.getListings() != null && !request.getListings().isEmpty()) {
            // Validate and add client listings to Redis
            List<RecentlyViewedItemDto> validListings = request.getListings().stream()
                    .filter(this::isValidTimestamp)
                    .collect(Collectors.toList());

            for (RecentlyViewedItemDto listing : validListings) {
                try {
                    // ZADD with score = timestamp, value = listingId
                    // This automatically handles duplicates - keeps the newest timestamp
                    zSetOps.add(redisKey, listing.getListingId().toString(), listing.getViewedAt().doubleValue());
                    log.debug("Added listing {} with timestamp {} for user {}",
                            listing.getListingId(), listing.getViewedAt(), userId);
                } catch (Exception e) {
                    log.warn("Failed to add listing {} for user {}: {}",
                            listing.getListingId(), userId, e.getMessage());
                }
            }
        }

        // Trim to keep only the most recent 20 listings
        // ZREMRANGEBYRANK removes listings with rank 0 to (totalCount - MAX_LISTINGS - 1)
        // This keeps the highest MAX_LISTINGS scores (most recent timestamps)
        Long totalCount = zSetOps.size(redisKey);
        if (totalCount != null && totalCount > MAX_LISTINGS) {
            long removeCount = totalCount - MAX_LISTINGS;
            zSetOps.removeRange(redisKey, 0, removeCount - 1);
            log.info("Trimmed recently viewed list for user {}: removed {} old listings", userId, removeCount);
        }

        // Evict recommendation cache if there are new interactions
        if (request.getListings() != null && !request.getListings().isEmpty()) {
            evictPersonalizedCache(userId);
        }

        // Return merged and sorted list
        return getRecentlyViewed();
    }

    @Override
    public List<RecentlyViewedItemResponse> getRecentlyViewed() {
        return getRecentlyViewed(getCurrentUserId());
    }

    @Override
    public List<RecentlyViewedItemResponse> getRecentlyViewed(String userId) {
        String redisKey = buildKey(userId);

        log.info("Retrieving recently viewed listings for user: {}", userId);

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // Get the top MAX_LISTINGS with highest scores (most recent) in reverse order
        // ZREVRANGE gets listings in descending order by score
        Set<ZSetOperations.TypedTuple<String>> listingsWithScores =
                zSetOps.reverseRangeWithScores(redisKey, 0, MAX_LISTINGS - 1);

        if (listingsWithScores == null || listingsWithScores.isEmpty()) {
            log.info("No recently viewed listings found for user: {}", userId);
            return new ArrayList<>();
        }

        // Extract listing IDs and their timestamps
        Map<Long, Long> listingIdToTimestamp = listingsWithScores.stream()
                .collect(Collectors.toMap(
                        tuple -> Long.parseLong(tuple.getValue()),
                        tuple -> tuple.getScore().longValue()
                ));

        // Fetch full listing details, restricted to listings that are currently
        // publicly displayed so hidden/expired/unverified ones are not surfaced.
        Set<Long> listingIds = listingIdToTimestamp.keySet();
        List<ListingResponse> listings = listingService.getDisplayingListingsByIds(listingIds);

        // Create a map for quick lookup
        Map<Long, ListingResponse> listingMap = listings.stream()
                .collect(Collectors.toMap(ListingResponse::getListingId, Function.identity()));

        // Build response preserving the order from Redis (most recent first)
        List<RecentlyViewedItemResponse> result = listingsWithScores.stream()
                .map(tuple -> {
                    Long listingId = Long.parseLong(tuple.getValue());
                    ListingResponse listing = listingMap.get(listingId);

                    // Only include if listing exists and is accessible
                    if (listing != null) {
                        return RecentlyViewedItemResponse.builder()
                                .listing(listing)
                                .viewedAt(tuple.getScore().longValue())
                                .build();
                    }
                    return null;
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());

        log.info("Retrieved {} recently viewed listings for user: {}", result.size(), userId);
        return result;
    }

    @Override
    public List<Long> getRecentlyViewedIds(String userId) {
        String redisKey = buildKey(userId);

        // ZREVRANGE returns the top MAX_LISTINGS listing IDs by score (most recent
        // first). Spring Data returns an insertion-ordered LinkedHashSet, so the
        // recency order is preserved. No DB hydration: callers that only need the
        // IDs skip getDisplayingListingsByIds (the full-DTO build for up to 20
        // listings) entirely.
        Set<String> ids = redisTemplate.opsForZSet().reverseRange(redisKey, 0, MAX_LISTINGS - 1);
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            try {
                result.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                log.warn("Skipping non-numeric recently-viewed entry '{}' for user {}", id, userId);
            }
        }
        return result;
    }

    @Override
    public java.util.LinkedHashMap<Long, Long> getRecentlyViewedIdsWithTimestamps(String userId) {
        String redisKey = buildKey(userId);

        // Same ZSET read as getRecentlyViewedIds, but keeping the scores. Spring
        // Data returns an insertion-ordered LinkedHashSet, so iterating the tuples
        // preserves the most-recent-first order into the LinkedHashMap.
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, MAX_LISTINGS - 1);

        java.util.LinkedHashMap<Long, Long> result = new java.util.LinkedHashMap<>();
        if (tuples == null || tuples.isEmpty()) {
            return result;
        }

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            try {
                result.put(Long.parseLong(tuple.getValue()), tuple.getScore().longValue());
            } catch (NumberFormatException e) {
                log.warn("Skipping non-numeric recently-viewed entry '{}' for user {}", tuple.getValue(), userId);
            }
        }
        return result;
    }

    /**
     * Validate timestamp to prevent invalid values
     * @param listing Listing to validate
     * @return true if timestamp is valid
     */
    private boolean isValidTimestamp(RecentlyViewedItemDto listing) {
        if (listing.getViewedAt() == null || listing.getViewedAt() < 0) {
            log.warn("Ignoring listing {} with negative or null timestamp: {}",
                    listing.getListingId(), listing.getViewedAt());
            return false;
        }

        if (listing.getViewedAt() > MAX_FUTURE_TIMESTAMP) {
            log.warn("Ignoring listing {} with far future timestamp: {}",
                    listing.getListingId(), listing.getViewedAt());
            return false;
        }

        return true;
    }

    /**
     * Get current authenticated user ID from SecurityContext
     * @return User ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /**
     * Build Redis key for user's recently viewed listings
     * @param userId User ID
     * @return Redis key
     */
    private String buildKey(String userId) {
        return KEY_PREFIX + userId;
    }
}