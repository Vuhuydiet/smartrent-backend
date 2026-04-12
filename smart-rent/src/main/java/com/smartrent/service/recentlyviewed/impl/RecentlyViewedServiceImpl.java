package com.smartrent.service.recentlyviewed.impl;

import com.smartrent.dto.request.RecentlyViewedItemRequest;
import com.smartrent.dto.request.RecentlyViewedSyncRequest;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.RecentlyViewedItemResponse;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.recentlyviewed.RecentlyViewedService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecentlyViewedServiceImpl implements RecentlyViewedService {

    ListingService listingService;
    StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_PREFIX = "recently_viewed:user:";
    private static final int MAX_RECENT_HISTORY = 30;

    @Override
    public List<RecentlyViewedItemResponse> sync(String userId, RecentlyViewedSyncRequest request) {
        log.info("Syncing recently viewed for user: {}", userId);
        String redisKey = REDIS_KEY_PREFIX + userId;

        if (request.getListings() != null) {
            for (RecentlyViewedItemRequest item : request.getListings()) {
                Long listingId = item.getListingId();
                Long viewedAtMillis = item.getViewedAt();

                // Only save to Redis
                redisTemplate.opsForZSet().add(redisKey, String.valueOf(listingId), viewedAtMillis);
                log.info("SYNC: Saved listingId {} to Redis for user {}", listingId, userId);
            }
        }

        // Trim Redis to keep only latest MAX_RECENT_HISTORY
        Long size = redisTemplate.opsForZSet().zCard(redisKey);
        if (size != null && size > MAX_RECENT_HISTORY) {
            redisTemplate.opsForZSet().removeRange(redisKey, 0, size - MAX_RECENT_HISTORY - 1);
        }

        return get(userId);
    }


    @Override
    public List<RecentlyViewedItemResponse> get(String userId) {
        String redisKey = REDIS_KEY_PREFIX + userId;
        
        Set<ZSetOperations.TypedTuple<String>> recentItems = redisTemplate.opsForZSet()
                .reverseRangeWithScores(redisKey, 0, MAX_RECENT_HISTORY - 1);

        if (recentItems == null || recentItems.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> listingIds = recentItems.stream()
                .map(tuple -> Long.parseLong(tuple.getValue()))
                .collect(Collectors.toSet());

        List<ListingResponse> listings = listingService.getListingsByIds(listingIds);
        Map<Long, ListingResponse> listingMap = listings.stream()
                .collect(Collectors.toMap(ListingResponse::getListingId, l -> l));

        List<RecentlyViewedItemResponse> responses = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : recentItems) {
            Long listingId = Long.parseLong(tuple.getValue());
            ListingResponse listingResponse = listingMap.get(listingId);
            if (listingResponse != null) {
                Long score = tuple.getScore() != null ? tuple.getScore().longValue() : System.currentTimeMillis();
                responses.add(new RecentlyViewedItemResponse(listingResponse, score));
            }
        }

        return responses;
    }
}
