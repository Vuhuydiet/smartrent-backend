package com.smartrent.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import redis.embedded.RedisServer;

class NotificationStreamConsumerTest {

  private static final String GROUP = "notification-workers";

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
    // Materialise the stream then create the group reading only new messages.
    redisTemplate.opsForStream().add(RedisStreamNotificationQueue.STREAM_KEY, Map.of("init", "1"));
    redisTemplate.opsForStream()
        .createGroup(RedisStreamNotificationQueue.STREAM_KEY, ReadOffset.latest(), GROUP);
  }

  @AfterEach
  void tearDown() {
    if (container != null) {
      container.stop();
    }
    connectionFactory.destroy();
  }

  @Test
  void consumesEnqueuedJob_andDispatchesToMatchingHandler() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<NotificationJob> received = new AtomicReference<>();
    NotificationHandler emailHandler = new NotificationHandler() {
      @Override
      public NotificationType supportedType() {
        return NotificationType.EMAIL;
      }

      @Override
      public void handle(NotificationJob job) {
        received.set(job);
        latch.countDown();
      }
    };
    NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(emailHandler));
    NotificationStreamConsumer consumer =
        new NotificationStreamConsumer(dispatcher, redisTemplate, GROUP);

    StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
        StreamMessageListenerContainerOptions.builder().pollTimeout(Duration.ofMillis(100)).build();
    container = StreamMessageListenerContainer.create(connectionFactory, options);
    container.receive(
        Consumer.from(GROUP, "worker-1"),
        StreamOffset.create(RedisStreamNotificationQueue.STREAM_KEY, ReadOffset.lastConsumed()),
        consumer);
    container.start();

    new RedisStreamNotificationQueue(redisTemplate)
        .enqueue(new NotificationJob(NotificationType.EMAIL, "{\"subject\":\"hi\"}"));

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(received.get().type()).isEqualTo(NotificationType.EMAIL);
    assertThat(received.get().payload()).isEqualTo("{\"subject\":\"hi\"}");
  }
}
