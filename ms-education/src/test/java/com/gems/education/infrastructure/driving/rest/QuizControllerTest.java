package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.QuizCommand;
import com.gems.education.application.command.QuizSubmissionCommand;
import com.gems.education.application.response.QuizGradingResponse;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.infrastructure.driving.rest.request.QuizRequest;
import com.gems.education.infrastructure.driving.rest.request.QuizSubmissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class QuizControllerTest {

  private CreateQuizUseCase createQuizUseCase;
  private GetQuizByLessonUseCase getQuizByLessonUseCase;
  private GetQuizByIdUseCase getQuizByIdUseCase;
  private UpdateQuizUseCase updateQuizUseCase;
  private DeleteQuizUseCase deleteQuizUseCase;
  private SubmitQuizUseCase submitQuizUseCase;
  private QuizController controller;

  @BeforeEach
  void setUp() {
    createQuizUseCase = mock(CreateQuizUseCase.class);
    getQuizByLessonUseCase = mock(GetQuizByLessonUseCase.class);
    getQuizByIdUseCase = mock(GetQuizByIdUseCase.class);
    updateQuizUseCase = mock(UpdateQuizUseCase.class);
    deleteQuizUseCase = mock(DeleteQuizUseCase.class);
    submitQuizUseCase = mock(SubmitQuizUseCase.class);

    controller = new QuizController(
      createQuizUseCase,
      getQuizByLessonUseCase,
      getQuizByIdUseCase,
      updateQuizUseCase,
      deleteQuizUseCase,
      submitQuizUseCase
    );
  }

  @Test
  void shouldCreateQuiz() {
    QuizResponse response = new QuizResponse(1L, 1L, "Quiz 1", 70, List.of());
    when(createQuizUseCase.execute(any(QuizCommand.class))).thenReturn(Mono.just(response));

    QuizRequest request = new QuizRequest(1L, "Quiz 1", 70, List.of());

    StepVerifier.create(controller.createQuiz(request))
      .assertNext(entity -> {
        assertEquals(201, entity.getStatusCode().value());
        assertEquals("Quiz 1", entity.getBody().getTitle());
      })
      .verifyComplete();
  }

  @Test
  void shouldGetQuizById() {
    QuizResponse response = new QuizResponse(1L, 1L, "Quiz 1", 70, List.of());
    when(getQuizByIdUseCase.execute(1L)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.getQuizById(1L))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Quiz 1", entity.getBody().getTitle());
      })
      .verifyComplete();
  }

  @Test
  void shouldGetQuizByLesson() {
    QuizResponse response = new QuizResponse(1L, 1L, "Quiz 1", 70, List.of());
    when(getQuizByLessonUseCase.execute(1L)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.getQuizByLesson(1L))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Quiz 1", entity.getBody().getTitle());
      })
      .verifyComplete();
  }

  @Test
  void shouldUpdateQuiz() {
    QuizResponse response = new QuizResponse(1L, 1L, "Updated Quiz", 70, List.of());
    when(updateQuizUseCase.execute(eq(1L), any(QuizCommand.class))).thenReturn(Mono.just(response));

    QuizRequest request = new QuizRequest(1L, "Updated Quiz", 70, List.of());

    StepVerifier.create(controller.updateQuiz(1L, request))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Updated Quiz", entity.getBody().getTitle());
      })
      .verifyComplete();
  }

  @Test
  void shouldDeleteQuiz() {
    when(deleteQuizUseCase.execute(1L)).thenReturn(Mono.empty());

    StepVerifier.create(controller.deleteQuiz(1L))
      .assertNext(entity -> {
        assertEquals(204, entity.getStatusCode().value());
      })
      .verifyComplete();
  }

  @Test
  void shouldSubmitQuiz() {
    QuizGradingResponse response = new QuizGradingResponse(80, true, 4, 5);
    when(submitQuizUseCase.execute(eq(1L), any(QuizSubmissionCommand.class))).thenReturn(Mono.just(response));

    QuizSubmissionRequest request = new QuizSubmissionRequest(10L, List.of(new QuizSubmissionRequest.AnswerRequest(1L, "A")));

    StepVerifier.create(controller.submitQuiz(1L, request))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals(80, entity.getBody().getScore());
      })
      .verifyComplete();
  }
}
