package com.smartrent.service.ai.impl;

import com.smartrent.service.ai.AiAnalysisQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Streams implementation of {@link AiAnalysisQueue}. Listing IDs are
 * appended to a single stream and consumed by a worker via a consumer group —
 * the same shape as the notification pipeline.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamAiAnalysisQueue implements AiAnalysisQueue {

    public static final String STREAM_KEY = "ai:listing:analysis";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void enqueue(Long listingId) {
        if (listingId == null) return;
        try {
            redisTemplate.opsForStream().add(STREAM_KEY, Map.of("listingId", String.valueOf(listingId)));
            log.debug("Enqueued listing {} for AI analysis", listingId);
        } catch (Exception e) {
            // Never fail the caller because the queue is down — the reconciliation
            // sweep re-discovers anything that never made it onto the stream.
            log.warn("Failed to enqueue listing {} for AI analysis (reconciliation sweep will retry): {}",
                    listingId, e.getMessage());
        }
    }
}
