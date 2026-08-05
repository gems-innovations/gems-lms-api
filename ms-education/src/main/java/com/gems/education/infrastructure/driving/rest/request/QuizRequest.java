package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class QuizRequest {
  @NotNull(message = "Lesson ID is required")
  private Long lessonId;

  @NotBlank(message = "Quiz title is required")
  private String title;

  @Min(value = 0, message = "Passing score cannot be negative")
  private Integer passingScore = 60;

  @Valid
  private List<QuestionRequest> questions;

  public QuizRequest() {
  }

  public QuizRequest(Long lessonId, String title, Integer passingScore, List<QuestionRequest> questions) {
    this.lessonId = lessonId;
    this.title = title;
    this.passingScore = passingScore;
    this.questions = questions;
  }

  public Long getLessonId() {
    return lessonId;
  }

  public void setLessonId(Long lessonId) {
    this.lessonId = lessonId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getPassingScore() {
    return passingScore;
  }

  public void setPassingScore(Integer passingScore) {
    this.passingScore = passingScore;
  }

  public List<QuestionRequest> getQuestions() {
    return questions;
  }

  public void setQuestions(List<QuestionRequest> questions) {
    this.questions = questions;
  }
}
