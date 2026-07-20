package com.smartrent.service.ratelimit;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Per-identity fixed-window request counter over Redis: INCRs
 * {@code <keyPrefix><identity>} and, only on the first hit of a window, sets
 * that key's TTL to {@code windowSeconds}. This is the shared counting logic
 * behind every per-identity rate limiter in the app (chat, price-suggestion,
 * ...) — each of those owns its own key prefix/limit/window so their buckets
 * never collide, but the INCR+EXPIRE mechanics only need to be correct once.
 *
 * <p>Not a Spring bean: each caller supplies its own key prefix and limits
 * (typically sourced from its own {@code @Value}-annotated constructor), so
 * a single shared singleton would not make sense here.
 */
public class FixedWindowRateLimiter {

  private final StringRedisTemplate redisTemplate;
  private final String keyPrefix;
  private final int max;
  private final int windowSeconds;

  public FixedWindowRateLimiter(
      StringRedisTemplate redisTemplate, String keyPrefix, int max, int windowSeconds) {
    this.redisTemplate = redisTemplate;
    this.keyPrefix = keyPrefix;
    this.max = max;
    this.windowSeconds = windowSeconds;
  }

  /** Returns true when the call is within the limit for the current window. */
  public boolean tryConsume(String identity) {
    String key = keyPrefix + identity;
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
    }
    return count != null && count <= max;
  }
}
