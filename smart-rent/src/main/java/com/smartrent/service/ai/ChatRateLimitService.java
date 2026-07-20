package com.smartrent.service.ai;

import com.smartrent.service.ratelimit.FixedWindowRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Per-identity fixed-window rate limiter for the chat SSE endpoint. Each chat
 * call costs LLM tokens, so this caps usage per user/IP on top of Caddy's
 * coarser per-IP gateway limit.
 *
 * <p>Thin wrapper around {@link FixedWindowRateLimiter}: this class exists so
 * chat keeps its own constructor, config properties and Redis key prefix
 * (unaffected by other limiters), while the INCR/EXPIRE mechanics live in one
 * shared place.
 */
@Service
public class ChatRateLimitService {

  private static final String KEY_PREFIX = "chat:ratelimit:";

  private final FixedWindowRateLimiter rateLimiter;

  public ChatRateLimitService(
      StringRedisTemplate redisTemplate,
      @Value("${chat.rate-limit.max-per-window:20}") int maxPerWindow,
      @Value("${chat.rate-limit.window-seconds:60}") int windowSeconds) {
    this.rateLimiter = new FixedWindowRateLimiter(redisTemplate, KEY_PREFIX, maxPerWindow, windowSeconds);
  }

  /** Returns true when the call is within the limit for the current window. */
  public boolean tryConsume(String identity) {
    return rateLimiter.tryConsume(identity);
  }
}
