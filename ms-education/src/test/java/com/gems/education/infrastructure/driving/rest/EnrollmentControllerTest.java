package com.gems.education.infrastructure.driving.rest;

import com.gems.education.TestData;
import com.gems.education.application.*;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.infrastructure.driving.rest.request.BulkEnrollmentRequest;
import com.gems.education.infrastructure.driving.rest.request.EnrollmentRequest;
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

class EnrollmentControllerTest extends ControllerTestSupport {
  private final EnrollStudentUseCase enrollStudentUseCase = mock(EnrollStudentUseCase.class);
  private final BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase = mock(BulkEnrollStudentsUseCase.class);
  private final GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase = mock(GetStudentEnrollmentsUseCase.class);
  private final GetEnrollmentsByCourseUseCase getEnrollmentsByCourseUseCase = mock(GetEnrollmentsByCourseUseCase.class);
  private final UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase = mock(UpdateEnrollmentProgressUseCase.class);
  private final DeleteEnrollmentUseCase deleteEnrollmentUseCase = mock(DeleteEnrollmentUseCase.class);
  private final GetEnrollmentsByInstitutionUseCase getEnrollmentsByInstitutionUseCase = mock(GetEnrollmentsByInstitutionUseCase.class);
  private EnrollmentController controller;

  /** Enrollment 20 belongs to the student (id 5) in course 1 of inst-1. */
  private final EnrollmentResponse studentEnrollment =
    TestData.enrollmentResponse(20L, STUDENT.userId(), 1L, LocalDateTime.now(), 0, null);

  @BeforeEach
  void setUp() {
    givenCourses();
    when(getEnrollmentByIdUseCase.execute(20L)).thenReturn(Mono.just(studentEnrollment));
    controller = new EnrollmentController(enrollStudentUseCase, bulkEnrollStudentsUseCase, getStudentEnrollmentsUseCase,
      getEnrollmentsByCourseUseCase, updateEnrollmentProgressUseCase, deleteEnrollmentUseCase, getEnrollmentByIdUseCase,
      getEnrollmentsByInstitutionUseCase, access);
  }

  private WebTestClient as(AuthenticatedUser caller) {
    return client(controller, caller);
  }

  @Test
  void studentEnrollsThemselves() {
    when(enrollStudentUseCase.execute(any())).thenReturn(Mono.just(studentEnrollment));

    as(STUDENT).post().uri("/api/v1/enrollments").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(new EnrollmentRequest(STUDENT.userId(), 1L))
      .exchange().expectStatus().isCreated()
      .expectBody().jsonPath("$.studentId").isEqualTo(5);
  }

  @Test
  void studentCannotEnrollSomeoneElse() {
    as(STUDENT).post().uri("/api/v1/enrollments").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(new EnrollmentRequest(99L, 1L))
      .exchange().expectStatus().isForbidden();
    verifyNoInteractions(enrollStudentUseCase);
  }

  @Test
  void bulkEnrollmentIsForStaffOfTheCourse() {
    when(bulkEnrollStudentsUseCase.execute(any())).thenReturn(Flux.just(studentEnrollment));
    BulkEnrollmentRequest request = new BulkEnrollmentRequest(List.of(5L, 6L), 1L);

    as(INSTRUCTOR).post().uri("/api/v1/enrollments/bulk").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isCreated();
    as(STUDENT).post().uri("/api/v1/enrollments/bulk").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isForbidden();
    as(OTHER_ADMIN).post().uri("/api/v1/enrollments/bulk").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isForbidden();
  }

  @Test
  void studentSeesOnlyTheirOwnEnrollments() {
    when(getStudentEnrollmentsUseCase.execute(5L)).thenReturn(Flux.just(studentEnrollment));

    as(STUDENT).get().uri("/api/v1/enrollments/student/5").exchange().expectStatus().isOk()
      .expectBodyList(EnrollmentResponse.class).hasSize(1);
    as(STUDENT).get().uri("/api/v1/enrollments/student/6").exchange().expectStatus().isForbidden();
  }

  @Test
  void staffOfAnotherInstitutionDoNotSeeTheseEnrollments() {
    when(getStudentEnrollmentsUseCase.execute(5L)).thenReturn(Flux.just(studentEnrollment));

    as(OTHER_ADMIN).get().uri("/api/v1/enrollments/student/5").exchange().expectStatus().isOk()
      .expectBodyList(EnrollmentResponse.class).hasSize(0);
  }

  @Test
  void studentUpdatesTheirOwnProgressWithDetail() {
    when(updateEnrollmentProgressUseCase.execute(eq(20L), eq(50), anyString())).thenReturn(Mono.just(studentEnrollment));

    as(STUDENT).put().uri("/api/v1/enrollments/20/progress").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"progress\":50,\"progressData\":\"{}\"}")
      .exchange().expectStatus().isOk();
    verify(updateEnrollmentProgressUseCase).execute(20L, 50, "{}");
  }

  @Test
  void anotherStudentCannotTouchTheProgress() {
    as(new AuthenticatedUser(6L, "STUDENT", "inst-1")).put().uri("/api/v1/enrollments/20/progress?progress=100")
      .exchange().expectStatus().isForbidden();
    verifyNoInteractions(updateEnrollmentProgressUseCase);
  }

  @Test
  void institutionEnrollmentsAreForItsStaff() {
    when(getEnrollmentsByInstitutionUseCase.execute("inst-1")).thenReturn(Flux.just(studentEnrollment));

    as(ADMIN).get().uri("/api/v1/enrollments/institution/inst-1").exchange().expectStatus().isOk()
      .expectBodyList(EnrollmentResponse.class).hasSize(1);
    as(STUDENT).get().uri("/api/v1/enrollments/institution/inst-1").exchange().expectStatus().isForbidden();
    as(OTHER_ADMIN).get().uri("/api/v1/enrollments/institution/inst-1").exchange().expectStatus().isForbidden();
  }

  @Test
  void onlyStaffDeleteEnrollments() {
    when(deleteEnrollmentUseCase.execute(20L)).thenReturn(Mono.empty());

    as(STUDENT).delete().uri("/api/v1/enrollments/20").exchange().expectStatus().isForbidden();
    as(ADMIN).delete().uri("/api/v1/enrollments/20").exchange().expectStatus().isNoContent();
  }
}
