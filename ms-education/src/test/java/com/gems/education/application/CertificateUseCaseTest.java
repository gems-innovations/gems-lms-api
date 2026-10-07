package com.gems.education.application;

import com.gems.education.TestData;
import com.gems.education.application.gateway.*;
import com.gems.education.domain.entities.Certificate;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.domain.entities.PathEnrollment;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CertificateUseCaseTest {
  private final CertificateGateway certificates = mock(CertificateGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final PathEnrollmentGateway pathEnrollments = mock(PathEnrollmentGateway.class);
  private final CourseGateway courses = mock(CourseGateway.class);
  private final LearningPathGateway paths = mock(LearningPathGateway.class);
  private final UserDirectory users = mock(UserDirectory.class);
  private final CertificateUseCase useCase = new CertificateUseCase(certificates, enrollments,
    pathEnrollments, courses, paths, users);

  @Test
  void issuesCredentialsOnlyForServerConfirmedCompletions() {
    var now = LocalDateTime.now();
    var completed = TestData.enrollment(1L, 5L, 10L, now.minusDays(2), 100, now);
    completed.setStatus("completed");
    var active = TestData.enrollment(2L, 5L, 11L, now, 50, null);
    var course = TestData.course(10L, "Curso", null, "published", "inst-1", now, now, List.of());
    course.setInstructorName("Docente");
    var path = new LearningPath(20L, "Ruta", null, "inst-1", now, List.of(course));
    var pathEnrollment = new PathEnrollment(3L, 20L, 5L, PathEnrollment.COMPLETED, now, now);

    when(users.find(5L)).thenReturn(Mono.just(new UserDirectory.UserProfile(5L, "Ana Ruiz", "inst-1")));
    when(enrollments.findByStudentId(5L)).thenReturn(Flux.just(completed, active));
    when(pathEnrollments.findByStudent(5L)).thenReturn(Flux.just(pathEnrollment));
    when(courses.findById(10L)).thenReturn(Mono.just(course));
    when(paths.findById(20L)).thenReturn(Mono.just(path));
    when(certificates.find(eq(5L), any(), any())).thenReturn(Mono.empty());
    when(certificates.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(certificates.findByStudent(5L)).thenReturn(Flux.empty());

    StepVerifier.create(useCase.sync(5L)).verifyComplete();
    var captured = org.mockito.ArgumentCaptor.forClass(Certificate.class);
    verify(certificates, times(2)).save(captured.capture());
    assertEquals(List.of("COURSE", "LEARNING_PATH"),
      captured.getAllValues().stream().map(Certificate::resourceType).sorted().toList());
    assertTrue(captured.getAllValues().stream().allMatch(c -> c.code().startsWith("GEMS-")));
    verify(courses, never()).findById(11L);
  }

  @Test
  void freeOpenCoursesDoNotIssueCertificates() {
    var now = LocalDateTime.now();
    var completed = TestData.enrollment(1L, 5L, 10L, now.minusDays(2), 100, now);
    completed.setStatus("completed");
    var openCourse = TestData.course(10L, "Curso gratis", null, "published", "gems-abierto", now, now, List.of());
    when(users.find(5L)).thenReturn(Mono.just(new UserDirectory.UserProfile(5L, "Ana Ruiz", "gems-abierto")));
    when(enrollments.findByStudentId(5L)).thenReturn(Flux.just(completed));
    when(pathEnrollments.findByStudent(5L)).thenReturn(Flux.empty());
    when(courses.findById(10L)).thenReturn(Mono.just(openCourse));
    when(certificates.findByStudent(5L)).thenReturn(Flux.empty());

    StepVerifier.create(useCase.withoutCertificatesFor("gems-abierto").sync(5L)).verifyComplete();
    verify(certificates, never()).save(any());
  }

  @Test
  void verificationRejectsRevokedCredentials() {
    var revoked = new Certificate(1L, "GEMS-ABC", 5L, "Ana", "inst-1", "COURSE", 10L,
      "Curso", null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    when(certificates.findByCode("GEMS-ABC")).thenReturn(Mono.just(revoked));
    StepVerifier.create(useCase.verify("gems-abc")).verifyComplete();
  }
}
