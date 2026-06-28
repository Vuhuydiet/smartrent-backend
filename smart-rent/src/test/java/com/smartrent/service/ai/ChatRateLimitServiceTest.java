package com.smartrent.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;

class ChatRateLimitServiceTest {

  private static RedisServer redisServer;
  private static int port;
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
    LettuceConnectionFactory cf = new LettuceConnectionFactory("localhost", port);
    cf.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(cf);
    redisTemplate.afterPropertiesSet();
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Test
  void tryConsume_allowsUpToLimitThenBlocks() {
    ChatRateLimitService service = new ChatRateLimitService(redisTemplate, 3, 60);

    assertThat(service.tryConsume("user-1")).isTrue();
    assertThat(service.tryConsume("user-1")).isTrue();
    assertThat(service.tryConsume("user-1")).isTrue();
    assertThat(service.tryConsume("user-1")).isFalse(); // 4th in window -> blocked
    assertThat(service.tryConsume("user-2")).isTrue(); // separate identity unaffected
  }
}
