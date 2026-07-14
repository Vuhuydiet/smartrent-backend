package com.smartrent.service.ai.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;

/**
 * Reads listing IDs delivered to the AI-analysis consumer group and runs the
 * store-only pre-computation for each, acknowledging every message when done.
 */
@Slf4j
public class AiAnalysisStreamConsumer
        implements StreamListener<String, MapRecord<String, String, String>> {

    private final AiAnalysisWorker worker;
    private final StringRedisTemplate redisTemplate;
    private final String group;

    public AiAnalysisStreamConsumer(
            AiAnalysisWorker worker, StringRedisTemplate redisTemplate, String group) {
        this.worker = worker;
        this.redisTemplate = redisTemplate;
        this.group = group;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            String raw = message.getValue().get("listingId");
            worker.analyze(Long.valueOf(raw));
        } catch (Exception e) {
            // Acknowledge below regardless: processSingleListing already tracks its own
            // retryCount, and the reconciliation sweep re-discovers anything left in
            // PENDING. Leaving the message unacked would only poison the consumer group.
            log.error("Failed to process queued AI analysis {}: {}", message.getId(), e.getMessage(), e);
        } finally {
            redisTemplate.opsForStream()
                    .acknowledge(RedisStreamAiAnalysisQueue.STREAM_KEY, group, message.getId());
        }
    }
}
