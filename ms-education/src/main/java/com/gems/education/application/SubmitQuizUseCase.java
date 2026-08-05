package com.gems.education.application;

import com.gems.education.application.command.QuizSubmissionCommand;
import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.application.response.QuizGradingResponse;
import com.gems.education.domain.entities.Question;
import com.gems.education.infrastructure.driving.rest.exeption.QuizNotFoundException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

public class SubmitQuizUseCase {
  private final QuizGateway quizGateway;

  public SubmitQuizUseCase(QuizGateway quizGateway) {
    this.quizGateway = quizGateway;
  }

  public Mono<QuizGradingResponse> execute(Long quizId, QuizSubmissionCommand command) {
    return quizGateway.findById(quizId)
      .switchIfEmpty(Mono.error(new QuizNotFoundException("Quiz not found with ID " + quizId)))
      .map(quiz -> {
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
          return new QuizGradingResponse(100, true, 0, 0);
        }

        Map<Long, String> submissions = command.getAnswers().stream()
          .filter(ans -> ans.getQuestionId() != null)
          .collect(Collectors.toMap(
            QuizSubmissionCommand.AnswerSubmission::getQuestionId,
            QuizSubmissionCommand.AnswerSubmission::getSelectedOption,
            (a, b) -> a // Keep first in case of duplicates
          ));

        int correctCount = 0;
        for (Question q : quiz.getQuestions()) {
          String selected = submissions.get(q.getId());
          if (selected != null && selected.trim().equalsIgnoreCase(q.getCorrectOption().trim())) {
            correctCount++;
          }
        }

        int totalCount = quiz.getQuestions().size();
        int score = (correctCount * 100) / totalCount;
        boolean passed = score >= quiz.getPassingScore();

        return new QuizGradingResponse(score, passed, correctCount, totalCount);
      });
  }
}
