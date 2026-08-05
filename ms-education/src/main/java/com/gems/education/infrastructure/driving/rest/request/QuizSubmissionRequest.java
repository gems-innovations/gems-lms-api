package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class QuizSubmissionRequest {
  @NotNull(message = "Student ID is required")
  private Long studentId;

  @NotEmpty(message = "Answers are required")
  @Valid
  private List<AnswerRequest> answers;

  public QuizSubmissionRequest() {
  }

  public QuizSubmissionRequest(Long studentId, List<AnswerRequest> answers) {
    this.studentId = studentId;
    this.answers = answers;
  }

  public Long getStudentId() {
    return studentId;
  }

  public void setStudentId(Long studentId) {
    this.studentId = studentId;
  }

  public List<AnswerRequest> getAnswers() {
    return answers;
  }

  public void setAnswers(List<AnswerRequest> answers) {
    this.answers = answers;
  }

  public static class AnswerRequest {
    @NotNull(message = "Question ID is required")
    private Long questionId;

    @NotBlank(message = "Selected option is required")
    private String selectedOption;

    public AnswerRequest() {
    }

    public AnswerRequest(Long questionId, String selectedOption) {
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
