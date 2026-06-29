package com.smartrent.service.notification;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes a {@link NotificationJob} to the {@link NotificationHandler} that
 * supports its type.
 */
public class NotificationDispatcher {

  private final Map<NotificationType, NotificationHandler> handlersByType;

  public NotificationDispatcher(List<NotificationHandler> handlers) {
    this.handlersByType = handlers.stream()
        .collect(Collectors.toMap(NotificationHandler::supportedType, Function.identity()));
  }

  public void dispatch(NotificationJob job) {
    NotificationHandler handler = handlersByType.get(job.type());
    if (handler == null) {
      throw new IllegalStateException("No handler registered for notification type: " + job.type());
    }
    handler.handle(job);
  }
}
