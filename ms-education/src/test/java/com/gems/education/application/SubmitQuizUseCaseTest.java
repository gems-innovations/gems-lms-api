package com.gems.education.application;

import com.gems.education.application.command.QuizSubmissionCommand;
import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.application.response.QuizGradingResponse;
import com.gems.education.domain.entities.Question;
import com.gems.education.domain.entities.Quiz;
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
class SubmitQuizUseCaseTest {

  @Mock
  private QuizGateway quizGateway;

  @InjectMocks
  private SubmitQuizUseCase submitQuizUseCase;

  @Test
  void shouldGradeQuizSuccessfully() {
    Quiz quiz = new Quiz(
      1L, 1L, "Java", 60,
      List.of(
        new Question(1L, 1L, "Q1", List.of("A", "B"), "A"),
        new Question(2L, 1L, "Q2", List.of("A", "B"), "B")
      )
    );

    QuizSubmissionCommand command = new QuizSubmissionCommand(
      10L,
      List.of(
        new QuizSubmissionCommand.AnswerSubmission(1L, "A"), // Correct
        new QuizSubmissionCommand.AnswerSubmission(2L, "A")  // Incorrect
      )
    );

    when(quizGateway.findById(1L)).thenReturn(Mono.just(quiz));

    Mono<QuizGradingResponse> result = submitQuizUseCase.execute(1L, command);

    StepVerifier.create(result)
      .expectNextMatches(res ->
        res.getScore() == 50 &&
          !res.isPassed() &&
          res.getCorrectCount() == 1 &&
          res.getTotalCount() == 2
      )
      .verifyComplete();

    verify(quizGateway, times(1)).findById(1L);
  }
}
