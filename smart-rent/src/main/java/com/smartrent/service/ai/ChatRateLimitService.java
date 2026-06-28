package com.smartrent.service.ai;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Per-identity fixed-window rate limiter for the chat SSE endpoint. Each chat
 * call costs LLM tokens, so this caps usage per user/IP on top of Caddy's
 * coarser per-IP gateway limit.
 */
@Service
public class ChatRateLimitService {

  private static final String KEY_PREFIX = "chat:ratelimit:";

  private final StringRedisTemplate redisTemplate;
  private final int maxPerWindow;
  private final int windowSeconds;

  public ChatRateLimitService(
      StringRedisTemplate redisTemplate,
      @Value("${chat.rate-limit.max-per-window:20}") int maxPerWindow,
      @Value("${chat.rate-limit.window-seconds:60}") int windowSeconds) {
    this.redisTemplate = redisTemplate;
    this.maxPerWindow = maxPerWindow;
    this.windowSeconds = windowSeconds;
  }

  /** Returns true when the call is within the limit for the current window. */
  public boolean tryConsume(String identity) {
    String key = KEY_PREFIX + identity;
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
    }
    return count != null && count <= maxPerWindow;
  }
}
