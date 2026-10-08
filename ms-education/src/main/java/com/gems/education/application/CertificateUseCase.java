package com.gems.education.application;

import com.gems.education.application.gateway.*;
import com.gems.education.domain.entities.Certificate;
import com.gems.education.domain.entities.PathEnrollment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public class CertificateUseCase {
  private final CertificateGateway certificates;
  private final EnrollmentGateway enrollments;
  private final PathEnrollmentGateway pathEnrollments;
  private final CourseGateway courses;
  private final LearningPathGateway paths;
  private final UserDirectory users;

  /** Set by the configuration; null in tests. */
  private NotificationUseCase notifications;

  public CertificateUseCase withNotifications(NotificationUseCase notifications) {
    this.notifications = notifications;
    return this;
  }

  /** Institución de los cursos gratis: no emiten certificado (muestran el resultado final en el reproductor). */
  private String openInstitutionId;

  public CertificateUseCase withoutCertificatesFor(String openInstitutionId) {
    this.openInstitutionId = openInstitutionId;
    return this;
  }

  public CertificateUseCase(CertificateGateway certificates, EnrollmentGateway enrollments,
      PathEnrollmentGateway pathEnrollments, CourseGateway courses, LearningPathGateway paths,
      UserDirectory users) {
    this.certificates = certificates;
    this.enrollments = enrollments;
    this.pathEnrollments = pathEnrollments;
    this.courses = courses;
    this.paths = paths;
    this.users = users;
  }

  /** Creates any missing credentials from server-confirmed completions, then returns the durable list. */
  public Flux<Certificate> sync(Long studentId) {
    return users.find(studentId).flatMapMany(profile -> Flux.merge(
        enrollments.findByStudentId(studentId)
          .filter(e -> "completed".equals(e.getStatus()) && e.getCompletedAt() != null)
          .flatMap(e -> courses.findHeaderById(e.getCourseId())
            .filter(course -> openInstitutionId == null || !openInstitutionId.equals(course.getInstitutionId()))
            .flatMap(course -> issue(profile,
            "COURSE", course.getId(), course.getTitle(), course.getInstructorName(), e.getCompletedAt()))),
        pathEnrollments.findByStudent(studentId)
          .filter(e -> PathEnrollment.COMPLETED.equals(e.status()) && e.completedAt() != null)
          .flatMap(e -> paths.findById(e.learningPathId()).flatMap(path -> issue(profile,
            "LEARNING_PATH", path.getId(), path.getTitle(), null, e.completedAt())))))
      .thenMany(certificates.findByStudent(studentId));
  }

  public Mono<Certificate> verify(String code) {
    return certificates.findByCode(code == null ? "" : code.trim().toUpperCase())
      .filter(Certificate::valid);
  }

  private Mono<Certificate> issue(UserDirectory.UserProfile user, String type, Long resourceId,
      String title, String instructor, LocalDateTime completedAt) {
    return certificates.find(user.id(), type, resourceId)
      .switchIfEmpty(Mono.defer(() -> certificates.save(new Certificate(null, code(), user.id(),
        user.fullName(), user.institutionId(), type, resourceId, title, instructor, completedAt,
        LocalDateTime.now(), null))
        .flatMap(saved -> notifications == null ? Mono.just(saved)
          : notifications.certificateIssued(user.id(), title, saved.code()).thenReturn(saved))));
  }

  private static String code() {
    return "GEMS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
  }
}
