package com.gems.education.application;

import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.application.response.QuestionResponse;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.infrastructure.driving.rest.exeption.QuizNotFoundException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetQuizByIdUseCase {
  private final QuizGateway quizGateway;

  public GetQuizByIdUseCase(QuizGateway quizGateway) {
    this.quizGateway = quizGateway;
  }

  public Mono<QuizResponse> execute(Long id) {
    return quizGateway.findById(id)
      .switchIfEmpty(Mono.error(new QuizNotFoundException("Quiz not found with ID: " + id)))
      .map(quiz -> {
        List<QuestionResponse> questions = new ArrayList<>();
        if (quiz.getQuestions() != null) {
          questions = quiz.getQuestions().stream()
            .map(q -> new QuestionResponse(q.getId(), q.getQuizId(), q.getText(), q.getOptions(), q.getCorrectOption()))
            .collect(Collectors.toList());
        }
        return new QuizResponse(quiz.getId(), quiz.getLessonId(), quiz.getTitle(), quiz.getPassingScore(), questions);
      });
  }
}
