package com.smartrent.service.predictor;

import com.smartrent.service.ratelimit.FixedWindowRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Per-identity fixed-window rate limiter for the price-suggestion endpoint.
 * Each call runs an LLM agent for up to 12 turns — far costlier than a single
 * chat turn — so the default cap is much tighter than chat's, and it uses its
 * own Redis key prefix so the two never share a budget.
 */
@Service
public class PriceSuggestionRateLimitService {

  private static final String KEY_PREFIX = "price-suggestion:ratelimit:";

  private final FixedWindowRateLimiter rateLimiter;

  public PriceSuggestionRateLimitService(
      StringRedisTemplate redisTemplate,
      @Value("${price-suggestion.rate-limit.max-per-window:5}") int maxPerWindow,
      @Value("${price-suggestion.rate-limit.window-seconds:60}") int windowSeconds) {
    this.rateLimiter = new FixedWindowRateLimiter(redisTemplate, KEY_PREFIX, maxPerWindow, windowSeconds);
  }

  /** Returns true when the call is within the limit for the current window. */
  public boolean tryConsume(String identity) {
    return rateLimiter.tryConsume(identity);
  }
}
