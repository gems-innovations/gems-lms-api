package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.infrastructure.driving.rest.request.LearningPathRequest;
import com.gems.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LearningPathControllerTest extends ControllerTestSupport {
  private final CreateLearningPathUseCase createLearningPathUseCase = mock(CreateLearningPathUseCase.class);
  private final GetLearningPathsByInstitutionUseCase getLearningPathsByInstitutionUseCase = mock(GetLearningPathsByInstitutionUseCase.class);
  private final UpdateLearningPathUseCase updateLearningPathUseCase = mock(UpdateLearningPathUseCase.class);
  private final DeleteLearningPathUseCase deleteLearningPathUseCase = mock(DeleteLearningPathUseCase.class);
  private final GetAllLearningPathsUseCase getAllLearningPathsUseCase = mock(GetAllLearningPathsUseCase.class);
  private LearningPathController controller;

  private static LearningPathResponse path(String institutionId) {
    return new LearningPathResponse(1L, "DevOps", "Ruta", institutionId, LocalDateTime.now(),
      List.of(course(1L, "published", institutionId)));
  }

  @BeforeEach
  void setUp() {
    when(getLearningPathByIdUseCase.execute(1L)).thenReturn(Mono.just(path("inst-1")));
    controller = new LearningPathController(createLearningPathUseCase, getLearningPathByIdUseCase,
      getLearningPathsByInstitutionUseCase, updateLearningPathUseCase, deleteLearningPathUseCase,
      getAllLearningPathsUseCase, access, studentView);
  }

  private WebTestClient as(AuthenticatedUser caller) {
    return client(controller, caller);
  }

  @Test
  void listIsScopedToTheCallersInstitution() {
    when(getLearningPathsByInstitutionUseCase.execute("inst-1")).thenReturn(Flux.just(path("inst-1")));

    as(STUDENT).get().uri("/api/v1/learning-paths").exchange().expectStatus().isOk()
      .expectBodyList(LearningPathResponse.class).hasSize(1);
    verifyNoInteractions(getAllLearningPathsUseCase);
  }

  @Test
  void superAdminListsEveryPath() {
    when(getAllLearningPathsUseCase.execute()).thenReturn(Flux.just(path("inst-1"), path("inst-2")));

    as(SUPER_ADMIN).get().uri("/api/v1/learning-paths").exchange().expectStatus().isOk()
      .expectBodyList(LearningPathResponse.class).hasSize(2);
  }

  @Test
  void studentsGetPathCoursesWithoutAnswerKeys() {
    String body = as(STUDENT).get().uri("/api/v1/learning-paths/1").exchange().expectStatus().isOk()
      .expectBody(String.class).returnResult().getResponseBody();

    assertEquals(false, body.contains("correctAnswer"));
  }

  @Test
  void otherInstitutionsCannotReadThePath() {
    as(OTHER_ADMIN).get().uri("/api/v1/learning-paths/1").exchange().expectStatus().isForbidden();
  }

  @Test
  void staffCreateOnlyInTheirInstitution() {
    when(createLearningPathUseCase.execute(any())).thenReturn(Mono.just(path("inst-1")));

    as(INSTRUCTOR).post().uri("/api/v1/learning-paths").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(new LearningPathRequest("DevOps", "Ruta", "inst-1", List.of(1L)))
      .exchange().expectStatus().isCreated();
    as(INSTRUCTOR).post().uri("/api/v1/learning-paths").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(new LearningPathRequest("DevOps", "Ruta", "inst-2", List.of(1L)))
      .exchange().expectStatus().isForbidden();
    as(STUDENT).post().uri("/api/v1/learning-paths").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(new LearningPathRequest("DevOps", "Ruta", "inst-1", List.of(1L)))
      .exchange().expectStatus().isForbidden();
  }

  @Test
  void staffUpdateAndDeleteTheirPaths() {
    when(updateLearningPathUseCase.execute(eq(1L), any())).thenReturn(Mono.just(path("inst-1")));
    when(deleteLearningPathUseCase.execute(1L)).thenReturn(Mono.empty());

    as(ADMIN).put().uri("/api/v1/learning-paths/1").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(new LearningPathRequest("DevOps 2", "Ruta", "inst-1", List.of(1L)))
      .exchange().expectStatus().isOk();
    as(OTHER_ADMIN).delete().uri("/api/v1/learning-paths/1").exchange().expectStatus().isForbidden();
    as(ADMIN).delete().uri("/api/v1/learning-paths/1").exchange().expectStatus().isNoContent();
  }
}
