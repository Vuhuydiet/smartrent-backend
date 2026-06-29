package com.smartrent.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;

class RedisStreamNotificationQueueTest {

  private static RedisServer redisServer;
  private static int port;

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redisTemplate;

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
    connectionFactory.destroy();
  }

  @Test
  void enqueue_addsEntryToStream_withTypeAndPayload() {
    RedisStreamNotificationQueue queue = new RedisStreamNotificationQueue(redisTemplate);

    queue.enqueue(new NotificationJob(NotificationType.EMAIL, "{\"subject\":\"hi\"}"));

    Long size = redisTemplate.opsForStream().size(RedisStreamNotificationQueue.STREAM_KEY);
    assertThat(size).isEqualTo(1L);

    List<MapRecord<String, Object, Object>> records =
        redisTemplate.opsForStream().range(RedisStreamNotificationQueue.STREAM_KEY, Range.unbounded());
    assertThat(records).hasSize(1);
    Map<Object, Object> value = records.get(0).getValue();
    assertThat(value.get("type")).isEqualTo("EMAIL");
    assertThat(value.get("payload")).isEqualTo("{\"subject\":\"hi\"}");
  }
}
