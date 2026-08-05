package com.gems.education.application;

import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.application.response.QuestionResponse;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.domain.entities.Quiz;
import com.gems.education.infrastructure.driving.rest.exeption.QuizNotFoundException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetQuizByLessonUseCase {
  private final QuizGateway quizGateway;

  public GetQuizByLessonUseCase(QuizGateway quizGateway) {
    this.quizGateway = quizGateway;
  }

  public Mono<QuizResponse> execute(Long lessonId) {
    return quizGateway.findByLessonId(lessonId)
      .map(this::mapToResponse)
      .switchIfEmpty(Mono.error(new QuizNotFoundException("Quiz not found for lesson ID " + lessonId)));
  }

  private QuizResponse mapToResponse(Quiz quiz) {
    List<QuestionResponse> questionResponses = new ArrayList<>();
    if (quiz.getQuestions() != null) {
      questionResponses = quiz.getQuestions().stream()
        .map(q -> new QuestionResponse(q.getId(), q.getQuizId(), q.getText(), q.getOptions(), q.getCorrectOption()))
        .collect(Collectors.toList());
    }

    return new QuizResponse(
      quiz.getId(),
      quiz.getLessonId(),
      quiz.getTitle(),
      quiz.getPassingScore(),
      questionResponses
    );
  }
}
