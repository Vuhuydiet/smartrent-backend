package com.smartrent.service.recentlyviewed.impl;

import com.smartrent.dto.request.RecentlyViewedItemDto;
import com.smartrent.dto.request.RecentlyViewedSyncRequest;
import com.smartrent.dto.response.RecentlyViewedItemResponse;
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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecentlyViewedServiceImpl implements RecentlyViewedService {

    private static final String KEY_PREFIX = "recently_viewed:";
    private static final int MAX_LISTINGS = 20;
    private static final long MAX_FUTURE_TIMESTAMP = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000); // 1 year in future

    private final RedisTemplate<String, String> redisTemplate;

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

        // Return merged and sorted list
        return getRecentlyViewed();
    }

    @Override
    public List<RecentlyViewedItemResponse> getRecentlyViewed() {
        String userId = getCurrentUserId();
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

        List<RecentlyViewedItemResponse> result = listingsWithScores.stream()
                .map(tuple -> RecentlyViewedItemResponse.builder()
                        .listingId(Long.parseLong(tuple.getValue()))
                        .viewedAt(tuple.getScore().longValue())
                        .build())
                .collect(Collectors.toList());

        log.info("Retrieved {} recently viewed listings for user: {}", result.size(), userId);
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