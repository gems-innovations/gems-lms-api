package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("quizzes")
public class QuizEntity {
  @Id
  private Long id;

  @Column("lesson_id")
  private Long lessonId;

  private String title;

  @Column("passing_score")
  private Integer passingScore;

  public QuizEntity() {
  }

  public QuizEntity(Long id, Long lessonId, String title, Integer passingScore) {
    this.id = id;
    this.lessonId = lessonId;
    this.title = title;
    this.passingScore = passingScore;
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
}
