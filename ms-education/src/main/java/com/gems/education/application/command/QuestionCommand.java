package com.gems.education.application.command;

import java.util.List;

public class QuestionCommand {
  private String text;
  private List<String> options;
  private String correctOption;

  public QuestionCommand() {
  }

  public QuestionCommand(String text, List<String> options, String correctOption) {
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
