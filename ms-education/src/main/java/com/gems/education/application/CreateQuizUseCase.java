package com.gems.education.application;

import com.gems.education.application.command.QuizCommand;
import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.application.response.QuestionResponse;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.domain.entities.Question;
import com.gems.education.domain.entities.Quiz;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreateQuizUseCase {
  private final QuizGateway quizGateway;

  public CreateQuizUseCase(QuizGateway quizGateway) {
    this.quizGateway = quizGateway;
  }

  public Mono<QuizResponse> execute(QuizCommand command) {
    List<Question> questions = new ArrayList<>();
    if (command.getQuestions() != null) {
      command.getQuestions().forEach(qCmd -> {
        questions.add(new Question(null, null, qCmd.getText(), qCmd.getOptions(), qCmd.getCorrectOption()));
      });
    }

    Quiz quiz = new Quiz(null, command.getLessonId(), command.getTitle(), command.getPassingScore(), questions);

    return quizGateway.save(quiz)
      .map(this::mapToResponse);
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
