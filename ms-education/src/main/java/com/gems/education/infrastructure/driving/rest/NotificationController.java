package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.NotificationUseCase;
import com.gems.education.domain.entities.Notification;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** The caller's notifications (their own and, for staff, their institution's staff notifications). */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
  private final NotificationUseCase notifications;

  public NotificationController(NotificationUseCase notifications) {
    this.notifications = notifications;
  }

  @GetMapping
  public Mono<ResponseEntity<List<NotificationResponse>>> mine() {
    return CurrentUser.get()
      .flatMap(caller -> notifications.forUser(caller.userId(), caller.institutionId(), caller.isStaff())
        .map(NotificationResponse::of).collectList())
      .map(ResponseEntity::ok);
  }

  @PutMapping("/{id}/read")
  public Mono<ResponseEntity<Void>> markRead(@PathVariable Long id) {
    return CurrentUser.get()
      .flatMap(caller -> notifications.find(id)
        .filter(n -> visibleTo(n, caller))
        .switchIfEmpty(Mono.error(new ForbiddenException("This notification is not yours")))
        .flatMap(n -> notifications.markRead(id, caller.userId())))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  @PutMapping("/read-all")
  public Mono<ResponseEntity<Void>> markAllRead() {
    return CurrentUser.get()
      .flatMap(caller -> notifications.markAllRead(caller.userId(), caller.institutionId(), caller.isStaff()))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  /**
   * Same fields as {@link Notification}, but {@code createdAt} carries its offset: the stored value is the server's
   * local time without a zone, and the browser would read it as its own local time (5 h off in Bogotá).
   */
  record NotificationResponse(Long id, String institutionId, Long recipientUserId, String type, String title,
                              String message, Long courseId, Long referenceId, OffsetDateTime createdAt, boolean read) {
    static NotificationResponse of(Notification n) {
      return new NotificationResponse(n.id(), n.institutionId(), n.recipientUserId(), n.type(), n.title(), n.message(),
        n.courseId(), n.referenceId(), n.createdAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(), n.read());
    }
  }

  private static boolean visibleTo(Notification n, AuthenticatedUser caller) {
    if (n.recipientUserId() != null) return caller.isUser(n.recipientUserId());
    return caller.isStaff() && caller.belongsTo(n.institutionId());
  }
}
