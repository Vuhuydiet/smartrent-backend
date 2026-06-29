package com.smartrent.service.notification;

import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Streams implementation of {@link NotificationQueue}. Jobs are appended
 * to a single stream and consumed by a worker via a consumer group.
 */
@Component
public class RedisStreamNotificationQueue implements NotificationQueue {

  public static final String STREAM_KEY = "notifications:stream";

  private final StringRedisTemplate redisTemplate;

  public RedisStreamNotificationQueue(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void enqueue(NotificationJob job) {
    Map<String, String> body = Map.of(
        "type", job.type().name(),
        "payload", job.payload());
    redisTemplate.opsForStream().add(STREAM_KEY, body);
  }
}
