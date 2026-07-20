package com.smartrent.service.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;

class FixedWindowRateLimiterTest {

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
    FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(redisTemplate, "test:ratelimit:", 3, 60);

    assertThat(limiter.tryConsume("user-1")).isTrue();
    assertThat(limiter.tryConsume("user-1")).isTrue();
    assertThat(limiter.tryConsume("user-1")).isTrue();
    assertThat(limiter.tryConsume("user-1")).isFalse(); // 4th in window -> blocked
    assertThat(limiter.tryConsume("user-2")).isTrue(); // separate identity unaffected
  }

  @Test
  void tryConsume_keyPrefixKeepsBucketsIsolated() {
    // Two limiters over the same identity but different prefixes (e.g. chat
    // vs price-suggestion) must not share a counter.
    FixedWindowRateLimiter chatLike = new FixedWindowRateLimiter(redisTemplate, "chat:ratelimit:", 1, 60);
    FixedWindowRateLimiter priceLike = new FixedWindowRateLimiter(redisTemplate, "price-suggestion:ratelimit:", 1, 60);

    assertThat(chatLike.tryConsume("same-identity")).isTrue();
    assertThat(chatLike.tryConsume("same-identity")).isFalse();

    // Exhausting the chat-like bucket must not affect the price-suggestion-like one.
    assertThat(priceLike.tryConsume("same-identity")).isTrue();
  }

  @Test
  void tryConsume_setsTtlOnlyOnFirstHit() {
    FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(redisTemplate, "ttl:ratelimit:", 5, 30);
    String key = "ttl:ratelimit:user-1";

    limiter.tryConsume("user-1");
    Long firstTtl = redisTemplate.getExpire(key);
    assertThat(firstTtl).isNotNull();
    assertThat(firstTtl).isGreaterThan(0).isLessThanOrEqualTo(30);

    limiter.tryConsume("user-1");
    Long secondTtl = redisTemplate.getExpire(key);
    // TTL was set once on the first INCR and must not be reset on subsequent hits.
    assertThat(secondTtl).isLessThanOrEqualTo(firstTtl);
  }
}
