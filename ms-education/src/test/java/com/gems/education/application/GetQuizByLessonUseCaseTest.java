package com.gems.education.application;

import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.domain.entities.Quiz;
import com.gems.education.infrastructure.driving.rest.exeption.QuizNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetQuizByLessonUseCaseTest {

  @Mock
  private QuizGateway quizGateway;

  @InjectMocks
  private GetQuizByLessonUseCase getQuizByLessonUseCase;

  @Test
  void shouldReturnQuizSuccessfully() {
    Quiz quiz = new Quiz(1L, 2L, "Quiz 2", 70, List.of());
    when(quizGateway.findByLessonId(2L)).thenReturn(Mono.just(quiz));

    Mono<QuizResponse> result = getQuizByLessonUseCase.execute(2L);

    StepVerifier.create(result)
      .expectNextMatches(res -> res.getId().equals(1L) && res.getLessonId().equals(2L))
      .verifyComplete();

    verify(quizGateway, times(1)).findByLessonId(2L);
  }

  @Test
  void shouldThrowNotFoundWhenNoQuizForLesson() {
    when(quizGateway.findByLessonId(3L)).thenReturn(Mono.empty());

    Mono<QuizResponse> result = getQuizByLessonUseCase.execute(3L);

    StepVerifier.create(result)
      .expectError(QuizNotFoundException.class)
      .verify();

    verify(quizGateway, times(1)).findByLessonId(3L);
  }
}
