package com.smartrent.service.predictor;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;

class PriceSuggestionRateLimitServiceTest {

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
    PriceSuggestionRateLimitService service = new PriceSuggestionRateLimitService(redisTemplate, 2, 60);

    assertThat(service.tryConsume("user-1")).isTrue();
    assertThat(service.tryConsume("user-1")).isTrue();
    assertThat(service.tryConsume("user-1")).isFalse(); // 3rd in window -> blocked
    assertThat(service.tryConsume("user-2")).isTrue(); // separate identity unaffected
  }

  @Test
  void tryConsume_usesOwnBucketSeparateFromChat() {
    // Same identity string, but chat's rate limiter must not see this service's
    // counter (or vice versa) since each uses a distinct Redis key prefix.
    PriceSuggestionRateLimitService priceLimiter = new PriceSuggestionRateLimitService(redisTemplate, 1, 60);
    com.smartrent.service.ai.ChatRateLimitService chatLimiter =
        new com.smartrent.service.ai.ChatRateLimitService(redisTemplate, 1, 60);

    assertThat(priceLimiter.tryConsume("shared-identity")).isTrue();
    assertThat(priceLimiter.tryConsume("shared-identity")).isFalse();

    assertThat(chatLimiter.tryConsume("shared-identity")).isTrue();
  }
}
