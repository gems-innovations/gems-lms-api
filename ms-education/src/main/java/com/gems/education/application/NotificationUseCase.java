package com.gems.education.application;

import com.gems.education.application.gateway.NotificationGateway;
import com.gems.education.domain.entities.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** In-app notifications: created by the server when something happens, read by each user. */
public class NotificationUseCase {
  private static final int LIMIT = 50;

  private final NotificationGateway gateway;

  public NotificationUseCase(NotificationGateway gateway) {
    this.gateway = gateway;
  }

  /** Tells the staff of the institution that a student delivered an assignment. */
  public Mono<Notification> submissionReceived(String institutionId, Long courseId, String courseTitle,
                                               Long submissionId) {
    return gateway.save(new Notification(null, institutionId, null, Notification.SUBMISSION,
      "Nueva entrega de tarea", "Un estudiante entregó una tarea en " + courseTitle, courseId, submissionId,
      LocalDateTime.now(), false));
  }

  /** Tells the student that their assignment was graded. */
  public Mono<Notification> submissionGraded(String institutionId, Long studentId, Long courseId, String courseTitle,
                                             Long submissionId, int grade) {
    return gateway.save(new Notification(null, institutionId, studentId, Notification.GRADED,
      "Tarea calificada", "Tu entrega en " + courseTitle + " fue calificada: " + grade, courseId, submissionId,
      LocalDateTime.now(), false));
  }

  public Flux<Notification> forUser(Long userId, String institutionId, boolean staff) {
    return gateway.findFor(userId, institutionId, staff, LIMIT);
  }

  public Mono<Notification> find(Long id) {
    return gateway.findById(id);
  }

  public Mono<Void> markRead(Long notificationId, Long userId) {
    return gateway.markRead(notificationId, userId);
  }

  public Mono<Void> markAllRead(Long userId, String institutionId, boolean staff) {
    return gateway.markAllRead(userId, institutionId, staff);
  }
}
