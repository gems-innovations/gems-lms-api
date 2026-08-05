package com.gems.education.application.command;

import java.util.List;

public class QuizSubmissionCommand {
  private Long studentId;
  private List<AnswerSubmission> answers;

  public QuizSubmissionCommand() {
  }

  public QuizSubmissionCommand(Long studentId, List<AnswerSubmission> answers) {
    this.studentId = studentId;
    this.answers = answers;
  }

  public Long getStudentId() {
    return studentId;
  }

  public void setStudentId(Long studentId) {
    this.studentId = studentId;
  }

  public List<AnswerSubmission> getAnswers() {
    return answers;
  }

  public void setAnswers(List<AnswerSubmission> answers) {
    this.answers = answers;
  }

  public static class AnswerSubmission {
    private Long questionId;
    private String selectedOption;

    public AnswerSubmission() {
    }

    public AnswerSubmission(Long questionId, String selectedOption) {
      this.questionId = questionId;
      this.selectedOption = selectedOption;
    }

    public Long getQuestionId() {
      return questionId;
    }

    public void setQuestionId(Long questionId) {
      this.questionId = questionId;
    }

    public String getSelectedOption() {
      return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
      this.selectedOption = selectedOption;
    }
  }
}
