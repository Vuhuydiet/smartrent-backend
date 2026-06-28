package com.smartrent.service.notification;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;

/**
 * Reads notification jobs delivered to the consumer group and hands them to the
 * {@link NotificationDispatcher}, acknowledging each message when done.
 */
@Slf4j
public class NotificationStreamConsumer
    implements StreamListener<String, MapRecord<String, String, String>> {

  private final NotificationDispatcher dispatcher;
  private final StringRedisTemplate redisTemplate;
  private final String group;

  public NotificationStreamConsumer(
      NotificationDispatcher dispatcher, StringRedisTemplate redisTemplate, String group) {
    this.dispatcher = dispatcher;
    this.redisTemplate = redisTemplate;
    this.group = group;
  }

  @Override
  public void onMessage(MapRecord<String, String, String> message) {
    try {
      Map<String, String> body = message.getValue();
      NotificationJob job =
          new NotificationJob(NotificationType.valueOf(body.get("type")), body.get("payload"));
      dispatcher.dispatch(job);
    } catch (Exception e) {
      // Acknowledge below regardless: the send path has its own retry, so a
      // failure here is logged rather than left to poison the consumer group.
      log.error("Failed to process notification {}: {}", message.getId(), e.getMessage(), e);
    } finally {
      redisTemplate.opsForStream()
          .acknowledge(RedisStreamNotificationQueue.STREAM_KEY, group, message.getId());
    }
  }
}
