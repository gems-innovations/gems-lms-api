package com.gems.education.application;

import com.gems.education.application.gateway.EmailNoticeGateway;
import com.gems.education.application.gateway.NotificationGateway;
import com.gems.education.domain.entities.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** In-app notifications: created by the server when something happens, read by each user. */
public class NotificationUseCase {
  private static final int LIMIT = 50;

  private final NotificationGateway gateway;
  private final EmailNoticeGateway email;

  public NotificationUseCase(NotificationGateway gateway) {
    this(gateway, EmailNoticeGateway.NONE);
  }

  /** Notices addressed to one person are also e-mailed (ms-auth applies their preferences). */
  public NotificationUseCase(NotificationGateway gateway, EmailNoticeGateway email) {
    this.gateway = gateway;
    this.email = email;
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
    String message = "Tu entrega en " + courseTitle + " fue calificada: " + grade;
    return gateway.save(new Notification(null, institutionId, studentId, Notification.GRADED,
      "Tarea calificada", message, courseId, submissionId, LocalDateTime.now(), false))
      .flatMap(saved -> email.send(java.util.List.of(studentId), "Tu tarea en " + courseTitle + " fue calificada",
        message + ".", "/learn/courses/" + courseId + "/grades", "Ver mi nota").thenReturn(saved));
  }

  /** One notification per enrolled student; referenceId is the announcement. */
  public Mono<Void> announcement(String institutionId, java.util.List<Long> studentIds, Long courseId, String courseTitle,
                                 Long announcementId, String title) {
    LocalDateTime now = LocalDateTime.now();
    return Flux.fromIterable(studentIds).distinct()
      .concatMap(id -> gateway.save(new Notification(null, institutionId, id, Notification.ANNOUNCEMENT,
        "Nuevo anuncio en " + courseTitle, title, courseId, announcementId, now, false)))
      .then(Mono.defer(() -> email.send(studentIds.stream().distinct().toList(), "Nuevo anuncio en " + courseTitle,
        title, "/learn/courses/" + courseId + "/community", "Ver el anuncio")));
  }

  /** Tells the staff of the institution that a student opened a forum thread; referenceId is the thread. */
  public Mono<Notification> forumThread(String institutionId, Long courseId, String courseTitle, Long threadId,
                                        String threadTitle) {
    return gateway.save(new Notification(null, institutionId, null, Notification.FORUM,
      "Nueva pregunta en el foro", courseTitle + ": " + threadTitle, courseId, threadId, LocalDateTime.now(), false));
  }

  /** Tells the thread author that someone replied. */
  public Mono<Notification> forumReply(String institutionId, Long authorId, Long courseId, String courseTitle,
                                       Long threadId, String threadTitle) {
    return gateway.save(new Notification(null, institutionId, authorId, Notification.FORUM,
      "Nueva respuesta en el foro", courseTitle + ": " + threadTitle, courseId, threadId, LocalDateTime.now(), false))
      .flatMap(saved -> email.send(java.util.List.of(authorId), "Te respondieron en el foro de " + courseTitle,
        "Hay una nueva respuesta en «" + threadTitle + "».", "/learn/courses/" + courseId + "/community",
        "Ver la respuesta").thenReturn(saved));
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
