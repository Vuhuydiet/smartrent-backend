package com.smartrent.service.ai.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;

import java.util.concurrent.Executor;

/**
 * Reads listing IDs delivered to the AI-analysis consumer group and runs the
 * store-only pre-computation for each, acknowledging every message when done.
 *
 * <p>Work is handed to {@code executor} rather than run inline. Spring Data's
 * {@code StreamMessageListenerContainer} calls {@link #onMessage} <b>synchronously
 * on the subscription's poll thread</b>, so doing a ~30s AI call here would stall
 * every other listing behind it — ten listings submitted at once would leave the
 * last one waiting five minutes. Dispatching lets several analyses overlap; the
 * pool is bounded with a caller-runs policy, so when it saturates the poll thread
 * runs the job itself and reading naturally stops — back-pressure instead of an
 * unbounded backlog.
 */
@Slf4j
public class AiAnalysisStreamConsumer
        implements StreamListener<String, MapRecord<String, String, String>> {

    private final AiAnalysisWorker worker;
    private final StringRedisTemplate redisTemplate;
    private final String group;
    private final Executor executor;

    public AiAnalysisStreamConsumer(
            AiAnalysisWorker worker,
            StringRedisTemplate redisTemplate,
            String group,
            Executor executor) {
        this.worker = worker;
        this.redisTemplate = redisTemplate;
        this.group = group;
        this.executor = executor;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        final Long listingId;
        try {
            listingId = Long.valueOf(message.getValue().get("listingId"));
        } catch (Exception e) {
            // Unparseable payload will never become parseable — ACK it away rather than
            // letting it sit pending forever.
            log.error("Malformed AI analysis message {}: {}", message.getId(), message.getValue(), e);
            acknowledge(message);
            return;
        }

        executor.execute(() -> {
            try {
                worker.analyze(listingId);
            } catch (Exception e) {
                // ACK below regardless: processSingleListing tracks its own retryCount, and
                // the reconciliation sweep re-discovers anything left PENDING. Leaving the
                // message unacked would only poison the consumer group.
                log.error("Failed to analyse queued listing {}: {}", listingId, e.getMessage(), e);
            } finally {
                acknowledge(message);
            }
        });
    }

    private void acknowledge(MapRecord<String, String, String> message) {
        try {
            redisTemplate.opsForStream()
                    .acknowledge(RedisStreamAiAnalysisQueue.STREAM_KEY, group, message.getId());
        } catch (Exception e) {
            log.warn("Failed to ACK AI analysis message {}: {}", message.getId(), e.getMessage());
        }
    }
}
