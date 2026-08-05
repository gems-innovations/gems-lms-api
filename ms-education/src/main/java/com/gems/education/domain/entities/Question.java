package com.gems.education.domain.entities;

import java.util.List;

public class Question {
  private Long id;
  private Long quizId;
  private String text;
  private List<String> options;
  private String correctOption;

  public Question() {
  }

  public Question(Long id, Long quizId, String text, List<String> options, String correctOption) {
    this.id = id;
    this.quizId = quizId;
    this.text = text;
    this.options = options;
    this.correctOption = correctOption;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getQuizId() {
    return quizId;
  }

  public void setQuizId(Long quizId) {
    this.quizId = quizId;
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
