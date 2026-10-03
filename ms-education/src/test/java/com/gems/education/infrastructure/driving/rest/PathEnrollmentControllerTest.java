package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.PathEnrollmentUseCase;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.PathEnrollment;
import com.gems.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PathEnrollmentControllerTest extends ControllerTestSupport {
  private final PathEnrollmentUseCase useCase = mock(PathEnrollmentUseCase.class);
  private PathEnrollmentController controller;

  private static LearningPathResponse path(String status) {
    LearningPathResponse path = new LearningPathResponse(1L, "DevOps", "Ruta", "inst-1", LocalDateTime.now(), List.of());
    path.setStatus(status);
    return path;
  }

  private static PathEnrollment enrollment(Long studentId) {
    return new PathEnrollment(30L, 1L, studentId, PathEnrollment.ACTIVE, LocalDateTime.now(), null);
  }

  @BeforeEach
  void setUp() {
    when(getLearningPathByIdUseCase.execute(1L)).thenReturn(Mono.just(path("published")));
    when(useCase.enroll(eq(1L), anyList())).thenAnswer(inv ->
      Flux.fromIterable(inv.<List<Long>>getArgument(1)).map(PathEnrollmentControllerTest::enrollment));
    controller = new PathEnrollmentController(useCase, access);
  }

  private WebTestClient as(AuthenticatedUser caller) {
    return client(controller, caller);
  }

  @Test
  void studentEnrollsThemselvesInAPublishedPath() {
    as(STUDENT).post().uri("/api/v1/learning-paths/1/enrollments").exchange()
      .expectStatus().isCreated()
      .expectBody().jsonPath("$[0].studentId").isEqualTo(5);
    verify(useCase).enroll(1L, List.of(5L));
  }

  @Test
  void studentsCannotEnrollInDraftsOrEnrollOthers() {
    as(STUDENT).post().uri("/api/v1/learning-paths/1/enrollments").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"studentIds\":[6]}")
      .exchange().expectStatus().isForbidden();

    when(getLearningPathByIdUseCase.execute(1L)).thenReturn(Mono.just(path("draft")));
    as(STUDENT).post().uri("/api/v1/learning-paths/1/enrollments").exchange().expectStatus().isForbidden();
    verify(useCase, never()).enroll(any(), anyList());
  }

  @Test
  void staffEnrollOthersOnlyInTheirInstitution() {
    as(INSTRUCTOR).post().uri("/api/v1/learning-paths/1/enrollments").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"studentIds\":[5,6]}")
      .exchange().expectStatus().isCreated()
      .expectBody().jsonPath("$.length()").isEqualTo(2);
    as(OTHER_ADMIN).post().uri("/api/v1/learning-paths/1/enrollments").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"studentIds\":[5]}")
      .exchange().expectStatus().isForbidden();
  }

  @Test
  void everyoneListsTheirOwnPathEnrollments() {
    when(useCase.progressOf(5L)).thenReturn(Flux.just(
      new PathEnrollmentUseCase.Progress(enrollment(5L), List.of(1L), 2L, 50)));

    as(STUDENT).get().uri("/api/v1/learning-paths/enrollments/me").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$[0].learningPathId").isEqualTo(1)
      .jsonPath("$[0].overallPercentage").isEqualTo(50)
      .jsonPath("$[0].currentCourseId").isEqualTo(2);
  }

  @Test
  void onlyStaffListWhoIsEnrolledInAPath() {
    when(useCase.ofPath(1L)).thenReturn(Flux.just(enrollment(5L)));

    as(ADMIN).get().uri("/api/v1/learning-paths/1/enrollments").exchange().expectStatus().isOk();
    as(STUDENT).get().uri("/api/v1/learning-paths/1/enrollments").exchange().expectStatus().isForbidden();
  }
}
