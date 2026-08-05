package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class QuestionRequest {
  @NotBlank(message = "Question text is required")
  private String text;

  @NotEmpty(message = "Question options are required")
  private List<String> options;

  @NotBlank(message = "Correct option is required")
  private String correctOption;

  public QuestionRequest() {
  }

  public QuestionRequest(String text, List<String> options, String correctOption) {
    this.text = text;
    this.options = options;
    this.correctOption = correctOption;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }

  public String getCorrectOption() {
    return correctOption;
  }

  public void setCorrectOption(String correctOption) {
    this.correctOption = correctOption;
  }
}
