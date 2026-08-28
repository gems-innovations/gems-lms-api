package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.LearningPathCommand;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.infrastructure.driving.rest.request.LearningPathRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LearningPathControllerTest {

  private CreateLearningPathUseCase createLearningPathUseCase;
  private GetLearningPathByIdUseCase getLearningPathByIdUseCase;
  private GetLearningPathsByInstitutionUseCase getLearningPathsByInstitutionUseCase;
  private UpdateLearningPathUseCase updateLearningPathUseCase;
  private DeleteLearningPathUseCase deleteLearningPathUseCase;
  private GetAllLearningPathsUseCase getAllLearningPathsUseCase;
  private LearningPathController controller;

  @BeforeEach
  void setUp() {
    createLearningPathUseCase = mock(CreateLearningPathUseCase.class);
    getLearningPathByIdUseCase = mock(GetLearningPathByIdUseCase.class);
    getLearningPathsByInstitutionUseCase = mock(GetLearningPathsByInstitutionUseCase.class);
    updateLearningPathUseCase = mock(UpdateLearningPathUseCase.class);
    deleteLearningPathUseCase = mock(DeleteLearningPathUseCase.class);
    getAllLearningPathsUseCase = mock(GetAllLearningPathsUseCase.class);

    controller = new LearningPathController(
      createLearningPathUseCase,
      getLearningPathByIdUseCase,
      getLearningPathsByInstitutionUseCase,
      updateLearningPathUseCase,
      deleteLearningPathUseCase,
      getAllLearningPathsUseCase
    );
  }

  @Test
  void shouldGetAllLearningPaths() {
    LearningPathResponse response = new LearningPathResponse(1L, "LP 1", "Desc", "inst-1", LocalDateTime.now(), List.of());
    when(getAllLearningPathsUseCase.execute()).thenReturn(Flux.just(response));

    StepVerifier.create(controller.getAllLearningPaths())
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        StepVerifier.create(entity.getBody())
          .assertNext(res -> assertEquals("LP 1", res.getTitle()))
          .verifyComplete();
      })
      .verifyComplete();
  }

  @Test
  void shouldCreateLearningPath() {
    LearningPathResponse response = new LearningPathResponse(1L, "LP 1", "Desc", "inst-1", LocalDateTime.now(), List.of());
    when(createLearningPathUseCase.execute(any(LearningPathCommand.class))).thenReturn(Mono.just(response));

    LearningPathRequest request = new LearningPathRequest("LP 1", "Desc", "inst-1", List.of());

    StepVerifier.create(controller.createLearningPath(request))
      .assertNext(entity -> {
        assertEquals(201, entity.getStatusCode().value());
        assertEquals("LP 1", entity.getBody().getTitle());
      })
      .verifyComplete();
  }

  @Test
  void shouldGetLearningPathById() {
    LearningPathResponse response = new LearningPathResponse(1L, "LP 1", "Desc", "inst-1", LocalDateTime.now(), List.of());
    when(getLearningPathByIdUseCase.execute(1L)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.getLearningPathById(1L))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("LP 1", entity.getBody().getTitle());
      })
      .verifyComplete();
  }

  @Test
  void shouldGetLearningPathsByInstitution() {
    LearningPathResponse response = new LearningPathResponse(1L, "LP 1", "Desc", "inst-1", LocalDateTime.now(), List.of());
    when(getLearningPathsByInstitutionUseCase.execute("inst-1")).thenReturn(Flux.just(response));

    StepVerifier.create(controller.getLearningPathsByInstitution("inst-1"))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        StepVerifier.create(entity.getBody())
          .assertNext(res -> assertEquals("LP 1", res.getTitle()))
          .verifyComplete();
      })
      .verifyComplete();
  }

  @Test
  void shouldUpdateLearningPath() {
    LearningPathResponse response = new LearningPathResponse(1L, "Updated LP", "Desc", "inst-1", LocalDateTime.now(), List.of());
    when(updateLearningPathUseCase.execute(eq(1L), any(LearningPathCommand.class))).thenReturn(Mono.just(response));

    LearningPathRequest request = new LearningPathRequest("Updated LP", "Desc", "inst-1", List.of());

    StepVerifier.create(controller.updateLearningPath(1L, request))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Updated LP", entity.getBody().getTitle());
      })
      .verifyComplete();
  }

  @Test
  void shouldDeleteLearningPath() {
    when(deleteLearningPathUseCase.execute(1L)).thenReturn(Mono.empty());

    StepVerifier.create(controller.deleteLearningPath(1L))
      .assertNext(entity -> {
        assertEquals(204, entity.getStatusCode().value());
      })
      .verifyComplete();
  }
}
