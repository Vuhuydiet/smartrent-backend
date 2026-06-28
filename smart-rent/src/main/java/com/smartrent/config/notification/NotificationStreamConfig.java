package com.smartrent.config.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.service.email.EmailService;
import com.smartrent.service.notification.EmailNotificationHandler;
import com.smartrent.service.notification.NotificationDispatcher;
import com.smartrent.service.notification.NotificationHandler;
import com.smartrent.service.notification.NotificationStreamConsumer;
import com.smartrent.service.notification.OtpNotificationHandler;
import com.smartrent.service.notification.RedisStreamNotificationQueue;
import com.smartrent.service.otp.provider.OtpProvider;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

/**
 * Wires the async notification pipeline: handlers, dispatcher, and a Redis
 * Streams consumer group that processes jobs off the request thread.
 *
 * <p>Gated by {@code notifications.async.enabled} (default on) so test/CI
 * contexts that have no Redis can disable the listener container.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    name = "notifications.async.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class NotificationStreamConfig {

  public static final String GROUP = "notification-workers";

  @Value("${notifications.consumer-name:worker-1}")
  private String consumerName;

  @Bean
  public EmailNotificationHandler emailNotificationHandler(
      EmailService emailService, ObjectMapper objectMapper) {
    return new EmailNotificationHandler(emailService, objectMapper);
  }

  @Bean
  public OtpNotificationHandler otpNotificationHandler(
      List<OtpProvider> providers, ObjectMapper objectMapper) {
    return new OtpNotificationHandler(providers, objectMapper);
  }

  @Bean
  public NotificationDispatcher notificationDispatcher(List<NotificationHandler> handlers) {
    return new NotificationDispatcher(handlers);
  }

  @Bean
  public NotificationStreamConsumer notificationStreamConsumer(
      NotificationDispatcher dispatcher, StringRedisTemplate redisTemplate) {
    return new NotificationStreamConsumer(dispatcher, redisTemplate, GROUP);
  }

  @Bean(destroyMethod = "stop")
  public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
      notificationStreamListenerContainer(
          RedisConnectionFactory connectionFactory,
          StringRedisTemplate redisTemplate,
          NotificationStreamConsumer consumer) {

    ensureConsumerGroup(redisTemplate);

    StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
        StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofSeconds(1))
            .build();

    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
        StreamMessageListenerContainer.create(connectionFactory, options);
    container.receive(
        Consumer.from(GROUP, consumerName),
        StreamOffset.create(RedisStreamNotificationQueue.STREAM_KEY, ReadOffset.lastConsumed()),
        consumer);
    container.start();
    log.info(
        "Notification stream consumer started: stream={}, group={}, consumer={}",
        RedisStreamNotificationQueue.STREAM_KEY, GROUP, consumerName);
    return container;
  }

  /** Idempotently create the consumer group (MKSTREAM), ignoring BUSYGROUP. */
  private void ensureConsumerGroup(StringRedisTemplate redisTemplate) {
    try {
      redisTemplate.execute((RedisCallback<String>) connection ->
          connection.streamCommands().xGroupCreate(
              RedisStreamNotificationQueue.STREAM_KEY.getBytes(StandardCharsets.UTF_8),
              GROUP,
              ReadOffset.from("0"),
              true));
      log.info("Created notification consumer group {}", GROUP);
    } catch (Exception e) {
      log.debug("Notification consumer group {} already present: {}", GROUP, e.getMessage());
    }
  }
}
