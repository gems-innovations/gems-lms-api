package com.gems.education.application.response;

import java.util.List;

public class QuizResponse {
  private Long id;
  private Long lessonId;
  private String title;
  private Integer passingScore;
  private List<QuestionResponse> questions;

  public QuizResponse() {
  }

  public QuizResponse(Long id, Long lessonId, String title, Integer passingScore, List<QuestionResponse> questions) {
    this.id = id;
    this.lessonId = lessonId;
    this.title = title;
    this.passingScore = passingScore;
    this.questions = questions;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public List<QuestionResponse> getQuestions() {
    return questions;
  }

  public void setQuestions(List<QuestionResponse> questions) {
    this.questions = questions;
  }
}
