package com.gems.education.application;

import com.gems.education.application.command.QuestionCommand;
import com.gems.education.application.command.QuizCommand;
import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.domain.entities.Question;
import com.gems.education.domain.entities.Quiz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateQuizUseCaseTest {

  @Mock
  private QuizGateway quizGateway;

  @InjectMocks
  private CreateQuizUseCase createQuizUseCase;

  private QuizCommand quizCommand;
  private Quiz savedQuiz;

  @BeforeEach
  void setUp() {
    quizCommand = new QuizCommand(
      1L,
      "Java Basics Quiz",
      80,
      List.of(new QuestionCommand("What is Java?", List.of("Language", "Coffee"), "Language"))
    );

    savedQuiz = new Quiz(
      1L,
      1L,
      "Java Basics Quiz",
      80,
      List.of(new Question(1L, 1L, "What is Java?", List.of("Language", "Coffee"), "Language"))
    );
  }

  @Test
  void shouldCreateQuizSuccessfully() {
    when(quizGateway.save(any(Quiz.class))).thenReturn(Mono.just(savedQuiz));

    Mono<QuizResponse> result = createQuizUseCase.execute(quizCommand);

    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.getId().equals(1L) &&
          response.getTitle().equals("Java Basics Quiz") &&
          response.getQuestions().size() == 1 &&
          response.getQuestions().get(0).getCorrectOption().equals("Language")
      )
      .verifyComplete();

    verify(quizGateway, times(1)).save(any(Quiz.class));
  }
}
