package com.gems.education.infrastructure.driving.rest.mapper;

import com.gems.education.application.command.QuestionCommand;
import com.gems.education.application.command.QuizCommand;
import com.gems.education.application.command.QuizSubmissionCommand;
import com.gems.education.infrastructure.driving.rest.request.QuizRequest;
import com.gems.education.infrastructure.driving.rest.request.QuizSubmissionRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuizMapper {
  private QuizMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static QuizCommand toCommand(QuizRequest request) {
    List<QuestionCommand> questions = new ArrayList<>();
    if (request.getQuestions() != null) {
      questions = request.getQuestions().stream()
        .map(q -> new QuestionCommand(q.getText(), q.getOptions(), q.getCorrectOption()))
        .collect(Collectors.toList());
    }
    return new QuizCommand(request.getLessonId(), request.getTitle(), request.getPassingScore(), questions);
  }

  public static QuizSubmissionCommand toCommand(QuizSubmissionRequest request) {
    List<QuizSubmissionCommand.AnswerSubmission> answers = new ArrayList<>();
    if (request.getAnswers() != null) {
      answers = request.getAnswers().stream()
        .map(ans -> new QuizSubmissionCommand.AnswerSubmission(ans.getQuestionId(), ans.getSelectedOption()))
        .collect(Collectors.toList());
    }
    return new QuizSubmissionCommand(request.getStudentId(), answers);
  }
}
