package com.gems.education.application;

import com.gems.education.application.command.QuizCommand;
import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.application.response.QuestionResponse;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.domain.entities.Question;
import com.gems.education.domain.entities.Quiz;
import com.gems.education.infrastructure.driving.rest.exeption.QuizNotFoundException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateQuizUseCase {
  private final QuizGateway quizGateway;

  public UpdateQuizUseCase(QuizGateway quizGateway) {
    this.quizGateway = quizGateway;
  }

  public Mono<QuizResponse> execute(Long id, QuizCommand command) {
    return quizGateway.findById(id)
      .switchIfEmpty(Mono.error(new QuizNotFoundException("Quiz not found with ID: " + id)))
      .flatMap(existing -> {
        List<Question> questions = new ArrayList<>();
        if (command.getQuestions() != null) {
          command.getQuestions().forEach(qCmd ->
            questions.add(new Question(null, null, qCmd.getText(), qCmd.getOptions(), qCmd.getCorrectOption()))
          );
        }
        Quiz updated = new Quiz(id, command.getLessonId(), command.getTitle(), command.getPassingScore(), questions);
        return quizGateway.save(updated);
      })
      .map(quiz -> {
        List<QuestionResponse> questionResponses = new ArrayList<>();
        if (quiz.getQuestions() != null) {
          questionResponses = quiz.getQuestions().stream()
            .map(q -> new QuestionResponse(q.getId(), q.getQuizId(), q.getText(), q.getOptions(), q.getCorrectOption()))
            .collect(Collectors.toList());
        }
        return new QuizResponse(quiz.getId(), quiz.getLessonId(), quiz.getTitle(), quiz.getPassingScore(), questionResponses);
      });
  }
}
