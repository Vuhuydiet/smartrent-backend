package com.smartrent.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationDispatcherTest {

  private static NotificationHandler handlerFor(NotificationType type, List<NotificationJob> sink) {
    return new NotificationHandler() {
      @Override
      public NotificationType supportedType() {
        return type;
      }

      @Override
      public void handle(NotificationJob job) {
        sink.add(job);
      }
    };
  }

  @Test
  void dispatch_routesJobToHandlerOfMatchingType() {
    List<NotificationJob> handled = new ArrayList<>();
    NotificationDispatcher dispatcher =
        new NotificationDispatcher(List.of(handlerFor(NotificationType.EMAIL, handled)));
    NotificationJob job = new NotificationJob(NotificationType.EMAIL, "{}");

    dispatcher.dispatch(job);

    assertThat(handled).containsExactly(job);
  }

  @Test
  void dispatch_throwsWhenNoHandlerRegisteredForType() {
    NotificationDispatcher dispatcher = new NotificationDispatcher(List.of());
    NotificationJob job = new NotificationJob(NotificationType.EMAIL, "{}");

    assertThatThrownBy(() -> dispatcher.dispatch(job))
        .isInstanceOf(IllegalStateException.class);
  }
}
