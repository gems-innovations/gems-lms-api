package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.NotificationUseCase;
import com.gems.education.domain.entities.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationControllerTest extends ControllerTestSupport {
  private final NotificationUseCase notifications = mock(NotificationUseCase.class);
  private NotificationController controller;

  private static Notification staffNotice() {
    return new Notification(1L, "inst-1", null, Notification.SUBMISSION, "Nueva entrega", "msg", 1L, 9L,
      LocalDateTime.now(), false);
  }

  private static Notification forStudent() {
    return new Notification(2L, "inst-1", 5L, Notification.GRADED, "Tarea calificada", "msg", 1L, 9L,
      LocalDateTime.now(), false);
  }

  @BeforeEach
  void setUp() {
    controller = new NotificationController(notifications);
    when(notifications.find(1L)).thenReturn(Mono.just(staffNotice()));
    when(notifications.find(2L)).thenReturn(Mono.just(forStudent()));
    when(notifications.markRead(anyLong(), anyLong())).thenReturn(Mono.empty());
  }

  @Test
  void staffGetTheirInstitutionsNotifications() {
    when(notifications.forUser(3L, "inst-1", true)).thenReturn(Flux.just(staffNotice()));

    client(controller, INSTRUCTOR).get().uri("/api/v1/notifications").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$[0].type").isEqualTo("submission");
  }

  @Test
  void studentsAskOnlyForTheirOwn() {
    when(notifications.forUser(5L, "inst-1", false)).thenReturn(Flux.just(forStudent()));

    client(controller, STUDENT).get().uri("/api/v1/notifications").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$[0].recipientUserId").isEqualTo(5);
    verify(notifications, never()).forUser(anyLong(), anyString(), eq(true));
  }

  @Test
  void usersMarkReadOnlyNotificationsMeantForThem() {
    client(controller, STUDENT).put().uri("/api/v1/notifications/2/read").exchange().expectStatus().isNoContent();
    client(controller, STUDENT).put().uri("/api/v1/notifications/1/read").exchange().expectStatus().isForbidden();
    client(controller, OTHER_ADMIN).put().uri("/api/v1/notifications/1/read").exchange().expectStatus().isForbidden();
    client(controller, ADMIN).put().uri("/api/v1/notifications/1/read").exchange().expectStatus().isNoContent();
    verify(notifications).markRead(2L, 5L);
    verify(notifications).markRead(1L, 2L);
  }
}
