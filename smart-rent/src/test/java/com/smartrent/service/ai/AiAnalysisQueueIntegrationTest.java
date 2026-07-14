package com.smartrent.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.smartrent.config.ai.AiAnalysisStreamConfig;
import com.smartrent.service.ai.impl.AiAnalysisStreamConsumer;
import com.smartrent.service.ai.impl.AiAnalysisWorker;
import com.smartrent.service.ai.impl.RedisStreamAiAnalysisQueue;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import redis.embedded.RedisServer;

/**
 * End-to-end proof that the AI analysis queue actually delivers: producer XADDs a
 * listing id, the consumer group hands it to the worker, and the message is ACKed
 * (so it is not redelivered forever).
 *
 * <p>Runs against a real embedded Redis — the plumbing (consumer group creation,
 * offsets, serialization) is exactly what breaks in production, and a mock would
 * prove none of it.
 */
class AiAnalysisQueueIntegrationTest {

  private static RedisServer redisServer;
  private static int port;

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redisTemplate;
  private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

  @BeforeAll
  static void startRedis() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    redisServer = RedisServer.newRedisServer().port(port).build();
    redisServer.start();
  }

  @AfterAll
  static void stopRedis() throws Exception {
    if (redisServer != null) {
      redisServer.stop();
    }
  }

  @BeforeEach
  void setUp() {
    connectionFactory = new LettuceConnectionFactory("localhost", port);
    connectionFactory.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @AfterEach
  void tearDown() {
    if (container != null) {
      container.stop();
    }
    connectionFactory.destroy();
  }

  /** Mirrors AiAnalysisStreamConfig.ensureConsumerGroup. */
  private void ensureConsumerGroup() {
    redisTemplate.execute(
        (RedisCallback<String>)
            connection ->
                connection
                    .streamCommands()
                    .xGroupCreate(
                        RedisStreamAiAnalysisQueue.STREAM_KEY.getBytes(StandardCharsets.UTF_8),
                        AiAnalysisStreamConfig.GROUP,
                        ReadOffset.from("0"),
                        true));
  }

  private void startConsumer(AiAnalysisWorker worker) {
    startConsumer(worker, Executors.newFixedThreadPool(2));
  }

  private void startConsumer(AiAnalysisWorker worker, Executor executor) {
    AiAnalysisStreamConsumer consumer =
        new AiAnalysisStreamConsumer(worker, redisTemplate, AiAnalysisStreamConfig.GROUP, executor);

    StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
        StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofMillis(100))
            .batchSize(1)
            .build();

    container = StreamMessageListenerContainer.create(connectionFactory, options);
    container.receive(
        Consumer.from(AiAnalysisStreamConfig.GROUP, "test-worker"),
        StreamOffset.create(RedisStreamAiAnalysisQueue.STREAM_KEY, ReadOffset.lastConsumed()),
        consumer);
    container.start();
  }

  @Test
  void enqueue_addsListingIdToStream() {
    RedisStreamAiAnalysisQueue queue = new RedisStreamAiAnalysisQueue(redisTemplate);

    queue.enqueue(123L);

    Long size = redisTemplate.opsForStream().size(RedisStreamAiAnalysisQueue.STREAM_KEY);
    assertThat(size).isEqualTo(1L);
  }

  @Test
  void enqueuedListing_isDeliveredToWorker_andAcknowledged() {
    ensureConsumerGroup();
    AiAnalysisWorker worker = mock(AiAnalysisWorker.class);
    startConsumer(worker);

    new RedisStreamAiAnalysisQueue(redisTemplate).enqueue(456L);

    // The worker actually gets the listing — this is the link that was never proven.
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(worker).analyze(456L));

    // ...and the message is ACKed, so it is not redelivered on every restart.
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              PendingMessagesSummary pending =
                  redisTemplate
                      .opsForStream()
                      .pending(
                          RedisStreamAiAnalysisQueue.STREAM_KEY, AiAnalysisStreamConfig.GROUP);
              assertThat(pending.getTotalPendingMessages()).isZero();
            });
  }

  /**
   * The container calls onMessage synchronously on its single poll thread, so without
   * the consumer offloading to a pool a slow analysis blocks every listing behind it.
   * Two slow listings must overlap, not run back-to-back.
   */
  @Test
  void slowAnalyses_runConcurrently_ratherThanBlockingEachOtherOnThePollThread()
      throws Exception {
    ensureConsumerGroup();

    CountDownLatch bothStarted = new CountDownLatch(2);
    CountDownLatch release = new CountDownLatch(1);
    AiAnalysisWorker worker = mock(AiAnalysisWorker.class);
    org.mockito.Mockito.doAnswer(
            inv -> {
              bothStarted.countDown();
              // Block until the test releases us. If analyses were serialised on the poll
              // thread, the second listing could never start and this latch never reaches 0.
              release.await(10, TimeUnit.SECONDS);
              return null;
            })
        .when(worker)
        .analyze(org.mockito.ArgumentMatchers.anyLong());

    startConsumer(worker, Executors.newFixedThreadPool(2));

    RedisStreamAiAnalysisQueue queue = new RedisStreamAiAnalysisQueue(redisTemplate);
    queue.enqueue(1L);
    queue.enqueue(2L);

    assertThat(bothStarted.await(10, TimeUnit.SECONDS))
        .as("both listings should be in-flight at the same time")
        .isTrue();

    release.countDown();
  }

  @Test
  void workerThrowing_stillAcknowledges_soOneBadListingCannotPoisonTheQueue() {
    ensureConsumerGroup();
    AiAnalysisWorker worker = mock(AiAnalysisWorker.class);
    org.mockito.Mockito.doThrow(new RuntimeException("AI service down"))
        .when(worker)
        .analyze(789L);
    startConsumer(worker);

    new RedisStreamAiAnalysisQueue(redisTemplate).enqueue(789L);

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> verify(worker).analyze(789L));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              PendingMessagesSummary pending =
                  redisTemplate
                      .opsForStream()
                      .pending(
                          RedisStreamAiAnalysisQueue.STREAM_KEY, AiAnalysisStreamConfig.GROUP);
              assertThat(pending.getTotalPendingMessages()).isZero();
            });
  }
}
