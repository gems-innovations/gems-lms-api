package com.gems.education.application;

import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.infrastructure.driving.rest.exeption.QuizNotFoundException;
import reactor.core.publisher.Mono;

public class DeleteQuizUseCase {
  private final QuizGateway quizGateway;

  public DeleteQuizUseCase(QuizGateway quizGateway) {
    this.quizGateway = quizGateway;
  }

  public Mono<Void> execute(Long id) {
    return quizGateway.findById(id)
      .switchIfEmpty(Mono.error(new QuizNotFoundException("Quiz not found with ID: " + id)))
      .flatMap(quiz -> quizGateway.deleteById(quiz.getId()));
  }
}
