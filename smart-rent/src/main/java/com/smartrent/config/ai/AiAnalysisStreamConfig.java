package com.smartrent.config.ai;

import com.smartrent.service.ai.impl.AiAnalysisStreamConsumer;
import com.smartrent.service.ai.impl.AiAnalysisWorker;
import com.smartrent.service.ai.impl.RedisStreamAiAnalysisQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Wires the AI-analysis queue: a Redis Streams consumer group that pre-computes a
 * listing's AI analysis as soon as it is submitted, off the request thread.
 *
 * <p>This is the primary trigger — the scheduler in
 * {@code AiListingAutoModerationScheduler} is only a reconciliation backstop.
 *
 * <p>Gated by {@code smartrent.ai.verification.queue.enabled} (default on) so
 * test/CI contexts without Redis can disable the listener container, mirroring
 * {@code notifications.async.enabled}.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    name = "smartrent.ai.verification.queue.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AiAnalysisStreamConfig {

    public static final String GROUP = "ai-analysis-workers";

    @Value("${smartrent.ai.verification.queue.consumer-name:worker-1}")
    private String consumerName;

    /**
     * How many listings may be analysed at once.
     *
     * <p>Kept small on purpose: the AI service is the bottleneck, not us. Flooding it
     * with concurrent multimodal requests buys no throughput and just pins its CPU.
     */
    @Value("${smartrent.ai.verification.queue.concurrency:4}")
    private int concurrency;

    /**
     * Bounded pool the consumer offloads analyses onto.
     *
     * <p>{@link ThreadPoolExecutor.CallerRunsPolicy} is the back-pressure: once the pool
     * and its queue are full, the poll thread runs the analysis itself, which stops it
     * reading more from the stream until it catches up. Unacked entries simply stay
     * pending in Redis — nothing is lost and nothing piles up in memory.
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor aiAnalysisQueuePool() {
        int size = concurrency > 0 ? concurrency : 4;
        return new ThreadPoolExecutor(
                size, size,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(size * 4),
                r -> {
                    Thread t = new Thread(r, "ai-queue-worker");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean
    public AiAnalysisStreamConsumer aiAnalysisStreamConsumer(
            AiAnalysisWorker worker,
            StringRedisTemplate redisTemplate,
            ThreadPoolExecutor aiAnalysisQueuePool) {
        return new AiAnalysisStreamConsumer(worker, redisTemplate, GROUP, aiAnalysisQueuePool);
    }

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
            aiAnalysisStreamListenerContainer(
                    RedisConnectionFactory connectionFactory,
                    StringRedisTemplate redisTemplate,
                    AiAnalysisStreamConsumer consumer) {

        ensureConsumerGroup(redisTemplate);

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        // The container calls the listener synchronously on this poll thread —
                        // the parallelism lives in the consumer's pool, not here.
                        .pollTimeout(Duration.ofSeconds(1))
                        .batchSize(1)
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);
        container.receive(
                Consumer.from(GROUP, consumerName),
                StreamOffset.create(RedisStreamAiAnalysisQueue.STREAM_KEY, ReadOffset.lastConsumed()),
                consumer);
        container.start();
        log.info(
                "AI analysis stream consumer started: stream={}, group={}, consumer={}",
                RedisStreamAiAnalysisQueue.STREAM_KEY, GROUP, consumerName);
        return container;
    }

    /** Idempotently create the consumer group (MKSTREAM), ignoring BUSYGROUP. */
    private void ensureConsumerGroup(StringRedisTemplate redisTemplate) {
        try {
            redisTemplate.execute((RedisCallback<String>) connection ->
                    connection.streamCommands().xGroupCreate(
                            RedisStreamAiAnalysisQueue.STREAM_KEY.getBytes(StandardCharsets.UTF_8),
                            GROUP,
                            ReadOffset.from("0"),
                            true));
            log.info("Created AI analysis consumer group {}", GROUP);
        } catch (Exception e) {
            log.debug("AI analysis consumer group {} already present: {}", GROUP, e.getMessage());
        }
    }
}
