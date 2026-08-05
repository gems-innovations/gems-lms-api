package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("questions")
public class QuestionEntity {
  @Id
  private Long id;

  @Column("quiz_id")
  private Long quizId;

  private String text;
  private String options; // Semi-colon separated options

  @Column("correct_option")
  private String correctOption;

  public QuestionEntity() {
  }

  public QuestionEntity(Long id, Long quizId, String text, String options, String correctOption) {
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

  public String getOptions() {
    return options;
  }

  public void setOptions(String options) {
    this.options = options;
  }

  public String getCorrectOption() {
    return correctOption;
  }

  public void setCorrectOption(String correctOption) {
    this.correctOption = correctOption;
  }
}
