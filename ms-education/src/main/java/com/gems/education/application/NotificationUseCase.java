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
      LocalDateTime.now(), false))
      .flatMap(saved -> email.sendToStaff(institutionId, "Nueva entrega en " + courseTitle,
        "Un estudiante entregó una tarea en " + courseTitle + ". Ya puedes revisarla y calificarla.",
        "/instructor/courses/" + courseId, "Revisar la entrega").thenReturn(saved));
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
      "Nueva pregunta en el foro", courseTitle + ": " + threadTitle, courseId, threadId, LocalDateTime.now(), false))
      .flatMap(saved -> email.sendToStaff(institutionId, "Nueva pregunta en el foro de " + courseTitle,
        "Un estudiante preguntó: «" + threadTitle + "». Responder pronto ayuda a que no se quede atascado.",
        "/instructor/courses/" + courseId, "Ver la pregunta").thenReturn(saved));
  }

  /** Welcome to a course: whether the student enrolled themselves or the staff enrolled them. */
  public Mono<Void> enrolled(Long studentId, Long courseId, String courseTitle, boolean byStaff) {
    String subject = byStaff ? "Te inscribieron en " + courseTitle : "Ya estás en " + courseTitle;
    String message = (byStaff ? "Tu institución te inscribió en «" + courseTitle + "». " : "Te inscribiste en «" + courseTitle + "». ")
      + "Empieza con la primera lección: son cortas y puedes hacerlas a tu ritmo.";
    return email.send(java.util.List.of(studentId), subject, message, "/learn/courses/" + courseId, "Empezar el curso");
  }

  /** The teacher's reminder, also by e-mail so it reaches students who stopped opening the app. */
  public Mono<Void> reminderEmail(Long studentId, Long courseId, String courseTitle, String message) {
    return email.send(java.util.List.of(studentId), "Recordatorio de " + courseTitle, message,
      "/learn/courses/" + courseId, "Retomar el curso");
  }

  /** A new certificate: course or learning path completed. */
  public Mono<Void> certificateIssued(Long studentId, String title, String code) {
    return email.send(java.util.List.of(studentId), "¡Completaste " + title + "!",
      "Terminaste «" + title + "» y tu certificado ya está listo. Cualquiera puede comprobarlo con el código " + code + ".",
      "/certificates/verify/" + code, "Ver mi certificado");
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
