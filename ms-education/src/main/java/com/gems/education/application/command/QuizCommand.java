package com.gems.education.application.command;

import java.util.List;

public class QuizCommand {
  private Long lessonId;
  private String title;
  private Integer passingScore;
  private List<QuestionCommand> questions;

  public QuizCommand() {
  }

  public QuizCommand(Long lessonId, String title, Integer passingScore, List<QuestionCommand> questions) {
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

  public List<QuestionCommand> getQuestions() {
    return questions;
  }

  public void setQuestions(List<QuestionCommand> questions) {
    this.questions = questions;
  }
}
