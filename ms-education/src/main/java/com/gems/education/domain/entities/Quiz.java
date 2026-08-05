package com.gems.education.domain.entities;

import java.util.List;

public class Quiz {
  private Long id;
  private Long lessonId;
  private String title;
  private Integer passingScore;
  private List<Question> questions;

  public Quiz() {
  }

  public Quiz(Long id, Long lessonId, String title, Integer passingScore, List<Question> questions) {
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

  public List<Question> getQuestions() {
    return questions;
  }

  public void setQuestions(List<Question> questions) {
    this.questions = questions;
  }
}
